package server;

import server.auth.AuthService;
import server.collection.CollectionManager;
import server.db.DatabaseManager;
import server.db.PersonDao;
import shared.exceptions.DatabaseException;
import shared.model.Person;

import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerApp {
    private static final Logger logger = Logger.getLogger(ServerApp.class.getName());
    private Server server;

    public void initialize() {
        try {
            setupLogging();
            logger.info("Инициализация сервера...");

            DatabaseManager.initialize();

            PersonDao personDao = new PersonDao();
            AuthService authService = new AuthService();
            CollectionManager collectionManager = new CollectionManager();

            HashSet<Person> dbData = personDao.loadAll();
            collectionManager.loadFromDB(dbData);

            server = new Server(collectionManager, personDao, authService);
            logger.info("Сервер успешно инициализирован");

        } catch (DatabaseException e) {
            logger.severe("Ошибка БД: " + e.getMessage());
            System.err.println("ОШИБКА БАЗЫ ДАННЫХ: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            logger.severe("Ошибка логирования: " + e.getMessage());
            System.err.println("ОШИБКА: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.severe("Неожиданная ошибка: " + e.getMessage());
            System.err.println("ОШИБКА: " + e.getMessage());
            System.exit(1);
        }
    }

    private void setupLogging() throws IOException {
        FileHandler fh = new FileHandler("server.log", true);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);
    }

    public void run() {
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                logger.severe("Ошибка сервера: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();
            if ("save".equalsIgnoreCase(command)) {
                System.out.println("Автосохранение в PostgreSQL включено. Команда save не требуется.");
            }
        }
    }

    public void shutdown() {
        if (server != null) {
            server.stop();
            server.close();
        }
        logger.info("Сервер остановлен");
    }

    public static void main(String[] args) {
        ServerApp app = new ServerApp();
        app.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
        app.run();
    }
}