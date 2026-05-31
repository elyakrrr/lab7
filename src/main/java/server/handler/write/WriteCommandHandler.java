package server.handler.write;

import server.collection.CollectionManager;
import server.commands.CommandType;
import server.db.PersonDao;
import shared.exceptions.DatabaseException;
import shared.exceptions.SecurityException;
import shared.model.Person;
import shared.network.Request;
import shared.network.Response;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ForkJoinPool;
import server.network.WritePacketTask;

public class WriteCommandHandler {
    private final CollectionManager collectionManager;
    private final PersonDao personDao;
    private final ForkJoinPool writePool;
    private final SocketAddress clientAddress;
    private final DatagramChannel channel;

    public WriteCommandHandler(CollectionManager cm, PersonDao dao,
                               ForkJoinPool writePool, SocketAddress addr, DatagramChannel ch) {
        this.collectionManager = cm;
        this.personDao = dao;
        this.writePool = writePool;
        this.clientAddress = addr;
        this.channel = ch;
    }

    public void handle(CommandType type, Request req, String creator) throws DatabaseException, SecurityException {
        Person person = req.getPerson();

        if (type == CommandType.UPDATE || type == CommandType.REMOVE_BY_ID) {
            validateOwnership(req, creator);
        }

        switch (type) {
            case ADD -> handleAdd(person, creator);
            case ADD_IF_MIN -> handleAddIfMin(person, creator);
            case UPDATE -> handleUpdate(req, person, creator);
            case REMOVE_BY_ID -> handleRemoveById(req);
            case CLEAR -> handleClear(creator);
            case REMOVE_GREATER -> handleRemoveGreater(person, creator);
            case REMOVE_LOWER -> handleRemoveLower(person, creator);
        }
    }

    private void validateOwnership(Request req, String creator) throws SecurityException {
        int id = Integer.parseInt(req.getArgs()[0]);
        if (!collectionManager.existsById(id)) {
            throw new IllegalArgumentException("Элемент с ID " + id + " не найден");
        }
        if (!collectionManager.canModify(id, creator)) {
            throw new SecurityException("Доступ запрещён: вы не являетесь создателем");
        }
    }

    private void handleAdd(Person person, String creator) throws DatabaseException {
        if (person == null) return;
        Integer dbId = personDao.insert(person);
        if (dbId != null) {
            person.setId(dbId);
            person.setCreator(creator);
            collectionManager.add(person);
            sendResponse("Элемент успешно добавлен. ID: " + dbId);
        } else throw new DatabaseException("Ошибка записи в БД");
    }

    private void handleAddIfMin(Person person, String creator) throws DatabaseException {
        if (person == null) return;
        if (!collectionManager.canAddIfMin(person)) {
            throw new IllegalArgumentException("Элемент не добавлен: его значение не меньше наименьшего");
        }
        handleAdd(person, creator);
    }

    private void handleUpdate(Request req, Person person, String creator) throws DatabaseException{
        if (person == null) throw new IllegalArgumentException("Для обновления требуется объект Person");
        int id = Integer.parseInt(req.getArgs()[0]);
        person.setId(id);
        person.setCreator(creator);
        personDao.updateById(person);
        collectionManager.update(id, person);
        sendResponse("Элемент успешно обновлён");
    }

    private void handleRemoveById(Request req) throws DatabaseException {
        int id = Integer.parseInt(req.getArgs()[0]);
        personDao.deleteById(id);
        collectionManager.removeById(id);
        sendResponse("Элемент успешно удалён");
    }

    private void handleClear(String creator) throws DatabaseException {
        personDao.clearByCreator(creator);
        int removed = collectionManager.clearByCreator(creator);
        String msg = removed > 0 ? "Удалено элементов: " + removed : "У вас нет элементов для удаления";
        sendResponse(msg);
    }

    private void handleRemoveGreater(Person person, String creator) throws DatabaseException {
        if (person == null) return;
        personDao.removeGreater(person, creator);
        int removed = collectionManager.removeGreater(person, creator);
        String msg = removed > 0 ? "Удалено элементов: " + removed : "Нет элементов больше заданного";
        sendResponse(msg);
    }

    private void handleRemoveLower(Person person, String creator) throws DatabaseException {
        if (person == null) return;
        personDao.removeLower(person, creator);
        int removed = collectionManager.removeLower(person, creator);
        String msg = removed > 0 ? "Удалено элементов: " + removed : "Нет элементов меньше заданного";
        sendResponse(msg);
    }

    private void sendResponse(String message) {
        Response resp = new Response.Builder().success(true).message(message).build();
        writePool.submit(new WritePacketTask(resp, clientAddress, channel));
    }
}