package server.handler.auth;

import server.auth.AuthService;
import server.commands.CommandType;
import shared.exceptions.DatabaseException;
import shared.exceptions.SecurityException;
import shared.network.Request;
import shared.network.Response;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import server.network.WritePacketTask;

public class AuthHandler {
    private final AuthService authService;
    private final ConcurrentHashMap<SocketAddress, String> sessions;
    private final ForkJoinPool writePool;
    private final SocketAddress clientAddress;
    private final DatagramChannel channel;

    public AuthHandler(AuthService authService, ConcurrentHashMap<SocketAddress, String> sessions,
                       ForkJoinPool writePool, SocketAddress clientAddress, DatagramChannel channel) {
        this.authService = authService;
        this.sessions = sessions;
        this.writePool = writePool;
        this.clientAddress = clientAddress;
        this.channel = channel;
    }

    public void handle(CommandType type, Request request) throws DatabaseException, SecurityException {
        String[] args = request.getArgs();
        if (args != null && args.length > 0 && "check".equals(args[0])) {
            handleLoginCheck(args);
            return;
        }
        handleAuth(type, request);
    }

    private void handleLoginCheck(String[] args) throws DatabaseException{
        String login = args.length > 1 ? args[1] : "";
        boolean exists = authService.isLoginExists(login);
        sendResponse(new Response.Builder()
                .success(!exists)
                .message(exists ? "Пользователь с таким логином уже существует" : "Логин свободен")
                .build());
    }

    private void handleAuth(CommandType type, Request request) throws DatabaseException, SecurityException {
        String login = request.getLogin();
        String password = request.getPassword();

        if (login == null || login.isEmpty() || password == null || password.isEmpty()) {
            throw new SecurityException("Логин и пароль обязательны");
        }

        String result = (type == CommandType.LOGIN)
                ? authService.login(login, password)
                : authService.register(login, password);

        sessions.put(clientAddress, result);
        String msg = (type == CommandType.LOGIN)
                ? "Авторизация успешна. Добро пожаловать, " + result
                : "Регистрация успешна. Добро пожаловать, " + result;
        sendResponse(new Response.Builder().success(true).message(msg).build());
    }

    private void sendResponse(Response response) {
        writePool.submit(new WritePacketTask(response, clientAddress, channel));
    }
}
