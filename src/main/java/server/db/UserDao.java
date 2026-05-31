package server.db;

import shared.exceptions.DatabaseException;
import shared.model.User;
import java.sql.*;
import java.util.logging.Logger;

public class UserDao {
    private static final Logger logger = Logger.getLogger(UserDao.class.getName());

    public User findUser(String username) throws DatabaseException {
        String sql = "SELECT username, password_hash FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("username"), rs.getString("password_hash"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска пользователя в БД", e);
        }
        return null;
    }

    public void registerUser(String username, String passwordHash) throws DatabaseException {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();
            logger.info("User registered: " + username);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка регистрации пользователя в БД", e);
        }
    }
}