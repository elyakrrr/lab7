package server.handler;

import server.auth.AuthService;
import server.collection.CollectionManager;
import server.commands.CommandInvoker;
import server.commands.CommandType;
import server.db.PersonDao;
import server.handler.auth.AuthHandler;
import server.handler.read.ReadCommandHandler;
import server.handler.write.WriteCommandHandler;
import shared.exceptions.DatabaseException;
import shared.exceptions.SecurityException;
import shared.network.Request;
import shared.network.Response;
import server.network.WritePacketTask;

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

public class RequestProcessorThread extends Thread {
    private static final Logger logger = Logger.getLogger(RequestProcessorThread.class.getName());

    private final Request request;
    private final SocketAddress clientAddress;
    private final DatagramChannel channel;
    private final ConcurrentHashMap<SocketAddress, String> sessions;
    private final ForkJoinPool writePool;

    private final AuthHandler authHandler;
    private final WriteCommandHandler writeHandler;
    private final ReadCommandHandler readHandler;

    public RequestProcessorThread(Request request, SocketAddress clientAddress, DatagramChannel channel,
                                  CollectionManager collectionManager, PersonDao personDao,
                                  AuthService authService, CommandInvoker commandInvoker,
                                  ForkJoinPool writePool, ConcurrentHashMap<SocketAddress, String> sessions) {
        this.request = request;
        this.clientAddress = clientAddress;
        this.channel = channel;
        this.sessions = sessions;
        this.writePool = writePool;

        this.authHandler = new AuthHandler(authService, sessions, writePool, clientAddress, channel);
        this.writeHandler = new WriteCommandHandler(collectionManager, personDao, writePool, clientAddress, channel);
        this.readHandler = new ReadCommandHandler(commandInvoker, collectionManager, writePool, clientAddress, channel);
    }

    @Override
    public void run() {
        System.out.println("[THREAD] Обработка в потоке: " + Thread.currentThread().getName());
        try {
            CommandType type = CommandType.fromString(request.getCommandName());
            if (type == null) {
                sendError("Неизвестная команда");
                return;
            }

            if (type == CommandType.LOGIN || type == CommandType.REGISTER) {
                authHandler.handle(type, request);
                return;
            }

            String creator = sessions.get(clientAddress);
            if (creator == null) {
                sendError("Вы не авторизованы. Введите 'login' или 'register'.");
                return;
            }
            if (request.getPerson() != null) {
                request.getPerson().setCreator(creator);
            }

            if (isWriteCommand(type)) {
                writeHandler.handle(type, request, creator);
            } else {
                readHandler.handle(type, request);
            }

        } catch (SecurityException e) { sendError(e.getMessage()); }
        catch (DatabaseException e) {
            logger.severe("Ошибка БД: " + e.getMessage());
            sendError("Ошибка базы данных");
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.info("Логическая ошибка: " + e.getMessage());
            sendError(e.getMessage());
        } catch (Exception e) {
            logger.severe("Неожиданная ошибка: " + e.getMessage());
            sendError("Внутренняя ошибка сервера");
        }
    }

    private boolean isWriteCommand(CommandType type) {
        return type == CommandType.ADD || type == CommandType.ADD_IF_MIN || type == CommandType.UPDATE ||
                type == CommandType.REMOVE_BY_ID || type == CommandType.REMOVE_GREATER ||
                type == CommandType.REMOVE_LOWER || type == CommandType.CLEAR;
    }

    private void sendError(String message) {
        Response resp = new Response.Builder().success(false).message(message).build();
        writePool.submit(new WritePacketTask(resp, clientAddress, channel));
    }
}