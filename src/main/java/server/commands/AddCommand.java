package server.commands;

import server.collection.CollectionManager;
import shared.model.Person;

public class AddCommand extends BaseCommand implements CommandInvoker.ElementCommand, CommandInvoker.ResultCapturingCommand {
    private CommandInvoker.CommandResult resultCapture;

    public AddCommand(CollectionManager collectionManager) {
        super(collectionManager, "add", "добавить новый элемент в коллекцию");
    }

    @Override
    public void execute(String[] args) {
        sendResult("Ошибка: команда add должна использоваться с элементом");
    }

    @Override
    public void executeWithElement(String[] args, Person person) {
        addPerson(person);
    }

    private void addPerson(Person person) {
        if (person == null) {
            sendResult("Ошибка: не передан объект Person");
            return;
        }

        collectionManager.add(person);

        String msg;
        if (person.getId() != null) {
            msg = "Элемент успешно добавлен. ID: " + person.getId();
        } else {
            msg = "Ошибка при добавлении элемента (не получен ID из БД)";
        }

        sendResult(msg);
    }

    private void sendResult(String message) {
        if (resultCapture != null) {
            resultCapture.append(message);
        }
    }

    @Override
    public void setResultCapture(CommandInvoker.CommandResult result) {
        this.resultCapture = result;
    }
}