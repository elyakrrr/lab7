package server.auth;

import server.db.UserDao;
import shared.exceptions.DatabaseException;
import shared.exceptions.SecurityException;
import shared.model.User;
import shared.utils.PasswordHasher;
import java.util.logging.Logger;

public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private final UserDao userDao;

    public AuthService() { this.userDao = new UserDao(); }

    public String register(String login, String password) throws DatabaseException, SecurityException {
        if (login == null || login.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new SecurityException("Логин и пароль не могут быть пустыми");
        }

        validatePassword(password);

        if (userDao.findUser(login) != null) {
            throw new SecurityException("Пользователь с таким логином уже существует");
        }
        userDao.registerUser(login, PasswordHasher.hash(password));
        logger.info("Зарегистрирован пользователь: " + login);
        return login;
    }

    public String login(String login, String password) throws DatabaseException, SecurityException {
        if (login == null || login.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new SecurityException("Логин и пароль обязательны");
        }
        User existing = userDao.findUser(login);
        if (existing == null) {
            throw new SecurityException("Пользователь не найден");
        }
        if (!PasswordHasher.verify(password, existing.getPasswordHash())) {
            throw new SecurityException("Неверный пароль");
        }
        return login;
    }

    private void validatePassword(String password) throws SecurityException {
        if (password.length() < 8) {
            throw new SecurityException("Пароль должен содержать минимум 8 символов");
        }
        if (!password.matches(".*[A-ZА-ЯЁ].*")) {
            throw new SecurityException("Пароль должен содержать хотя бы одну заглавную букву");
        }
        if (!password.matches(".*\\d.*")) {
            throw new SecurityException("Пароль должен содержать хотя бы одну цифру");
        }
        if (!password.matches(".*[^a-zA-Z0-9А-Яа-яёЁ].*")) {
            throw new SecurityException("Пароль должен содержать хотя бы один спецсимвол (!@#$%^&* и т.д.)");
        }
    }

    public boolean isLoginExists(String login) throws DatabaseException {
        return userDao.findUser(login) != null;
    }
}