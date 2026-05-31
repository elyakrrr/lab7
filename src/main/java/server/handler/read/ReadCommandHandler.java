package server.handler.read;

import server.collection.CollectionManager;
import server.commands.CommandInvoker;
import server.commands.CommandType;
import shared.network.Request;
import shared.network.Response;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ForkJoinPool;
import server.network.WritePacketTask;

public class ReadCommandHandler {
    private final CommandInvoker commandInvoker;
    private final CollectionManager collectionManager;
    private final ForkJoinPool writePool;
    private final SocketAddress clientAddress;
    private final DatagramChannel channel;

    public ReadCommandHandler(CommandInvoker invoker, CollectionManager cm,
                              ForkJoinPool writePool, SocketAddress addr, DatagramChannel ch) {
        this.commandInvoker = invoker;
        this.collectionManager = cm;
        this.writePool = writePool;
        this.clientAddress = addr;
        this.channel = ch;
    }

    public void handle(CommandType type, Request req) {
        Object result = commandInvoker.executeCommand(type, req.getArgs(), req.getPerson());
        Response response = buildResponse(type, result);
        sendResponse(response);
    }

    private Response buildResponse(CommandType type, Object result) {
        Response.Builder b = new Response.Builder().success(true);
        switch (type) {
            case SHOW -> b.collection(collectionManager.getSortedByLocation());
            case INFO -> b.info(collectionManager.getInfo());
            case PRINT_UNIQUE_NATIONALITY,
                 PRINT_FIELD_DESCENDING_BIRTHDAY,
                 COUNT_LESS_THAN_NATIONALITY -> b.data(result);
            default -> b.message(result != null ? result.toString() : "Команда выполнена");
        }
        return b.build();
    }

    private void sendResponse(Response response) {
        writePool.submit(new WritePacketTask(response, clientAddress, channel));
    }
}