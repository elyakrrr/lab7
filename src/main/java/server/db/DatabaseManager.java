package server.db;

import shared.exceptions.DatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private static final String URL = "jdbc:postgresql://pg:5432/studs";
    private static final String USER = "s503268";
    private static final String PASSWORD = "HdiEv7GkQnSiagdG";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось получить соединение с БД: " + e.getMessage(), e);
        }
    }

    public static void initialize() throws DatabaseException {

        String[] initQueries = {

                "CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY CHECK (trim(username) <> ''), password_hash TEXT NOT NULL)",
                "CREATE SEQUENCE IF NOT EXISTS person_id_seq START 1",
                "CREATE TABLE IF NOT EXISTS persons (" +
                        "id INTEGER DEFAULT nextval('person_id_seq') PRIMARY KEY CHECK (id > 0), " +
                        "name TEXT NOT NULL CHECK (trim(name) <> ''), " +
                        "coord_x INTEGER NOT NULL CHECK (coord_x > -746), " +
                        "coord_y BIGINT NOT NULL, " +
                        "creation_date DATE NOT NULL DEFAULT CURRENT_DATE, " +
                        "height REAL NOT NULL CHECK (height > 0), " +
                        "birthday DATE, " +
                        "hair_color TEXT CHECK (hair_color IN ('GREEN', 'RED', 'BLUE', 'ORANGE', 'BROWN') OR hair_color IS NULL), " +
                        "nationality TEXT CHECK (nationality IN ('THAILAND', 'SOUTH_KOREA', 'JAPAN') OR nationality IS NULL), " +
                        "loc_x REAL, " +
                        "loc_y BIGINT, " +
                        "loc_name TEXT, " +
                        "creator TEXT NOT NULL CHECK (trim(creator) <> '')" +
                        ")"
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            logger.info("Начало инициализации схемы БД...");

            for (int i = 0; i < initQueries.length; i++) {
                String query = initQueries[i];
                logger.info("Выполняю запрос #" + (i+1) + ": " + query.substring(0, Math.min(80, query.length())) + "...");
                stmt.execute(query);
            }

            conn.commit();
            logger.info("Схема БД успешно инициализирована");

        } catch (SQLException e) {
            logger.severe("ОШИБКА при инициализации БД:");
            logger.severe("Message: " + e.getMessage());
            logger.severe("SQLState: " + e.getSQLState());
            logger.severe("ErrorCode: " + e.getErrorCode());
            throw new DatabaseException("Ошибка инициализации схемы БД: " + e.getMessage(), e);
        }
    }
}