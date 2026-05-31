package client;

import client.auth.AuthManager;
import client.io.InputCancelledException;
import client.io.NonClosingInputStream;
import client.io.ScriptExecutor;
import client.io.UserInputReader;
import client.handler.ResponseHandler;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

public class ClientApp {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8081;

    private final Client client;
    private final UserInputReader inputReader;
    private final ScriptExecutor scriptExecutor;
    private final Scanner scanner;
    private final NonClosingInputStream nonClosingInputStream;
    private boolean running;

    public ClientApp(String host, int port) throws SocketException {
        this.client = new Client(host, port);
        this.nonClosingInputStream = new NonClosingInputStream(System.in);
        this.scanner = new Scanner(nonClosingInputStream);
        this.inputReader = new UserInputReader(scanner, nonClosingInputStream);
        this.scriptExecutor = new ScriptExecutor(this);
        this.running = true;
    }

    public void start() {
        new AuthManager(client, scanner, nonClosingInputStream).authenticate();

        ResponseHandler responseHandler = new ResponseHandler();
        CommandProcessor commandProcessor = new CommandProcessor(client, inputReader, responseHandler);

        printWelcomeMessage();

        while (running) {
            try {
                processUserInput(commandProcessor);
            } catch (IOException | ClassNotFoundException e) {
                handleNetworkError(e);
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
        shutdown();
    }

    private void printWelcomeMessage() {
        System.out.println("Клиент запущен. Сервер: " + client.getHost() + ":" + client.getPort());
        System.out.println("Введите 'help' для списка команд");
        System.out.println("Введите 'exit' для выхода");
    }

    private void processUserInput(CommandProcessor commandProcessor) throws IOException, ClassNotFoundException {
        System.out.print("> ");

        String input = scanner.nextLine();

        if (nonClosingInputStream.wasEofSignal()) {
            nonClosingInputStream.clearEofSignal();
            System.out.println("\n[Ctrl+D] Для выхода введите 'exit'.");
            return;
        }

        input = input.trim();
        if (input.isEmpty()) return;

        String[] parts = input.split("\\s+");
        String commandName = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        if (!handleSpecialCommand(commandName, args)) {
            try {
                commandProcessor.process(commandName, args);
            } catch (InputCancelledException e) {
                System.out.println("Команда отменена. Возврат в главное меню.");
            }
        }
    }

    private void handleNetworkError(Exception e) {
        System.err.println("Ошибка сети: " + e.getMessage());
        System.out.println("Попробуйте снова позже или проверьте подключение к серверу");
    }

    private boolean handleSpecialCommand(String commandName, String[] args) {
        return switch (commandName) {
            case "exit"           -> { handleExit(); yield true; }
            case "save"           -> { handleSave(); yield true; }
            case "execute_script" -> { handleExecuteScript(args); yield true; }
            default               -> false;
        };
    }

    private void handleExit() {
        running = false;
        System.out.println("Завершение клиентского приложения...");
    }

    private void handleSave() {
        System.out.println("Команда save недоступна на клиенте");
    }

    private void handleExecuteScript(String[] args) {
        if (args.length != 1) {
            System.out.println("Использование: execute_script file_name");
            return;
        }
        scriptExecutor.execute(args[0]);
    }

    private void shutdown() {
        client.close();
        scanner.close();
        System.out.println("Клиент завершён.");
    }

    public void stop() { this.running = false; }
    public Client getClient() { return client; }

    public static void main(String[] args) {
        try {
            new ClientApp(DEFAULT_HOST, DEFAULT_PORT).start();
        } catch (SocketException e) {
            System.err.println("Ошибка создания клиента: " + e.getMessage());
        }
    }
}