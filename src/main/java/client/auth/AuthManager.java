package client.auth;

import client.Client;
import client.io.InputCancelledException;
import client.io.NonClosingInputStream;
import shared.network.Response;
import java.io.IOException;
import java.util.Scanner;

public class AuthManager {
    private final Client client;
    private final Scanner scanner;
    private final NonClosingInputStream inputStream;
    private String lastAuthErrorMessage = null;

    public AuthManager(Client client, Scanner scanner, NonClosingInputStream inputStream) {
        this.client = client;
        this.scanner = scanner;
        this.inputStream = inputStream;
    }

    public void authenticate() {
        System.out.println("Идентификация пользователя");
        System.out.println("Доступные команды: login, register");

        while (true) {
            System.out.print("Введите команду (login/register): ");
            String cmd;
            try {
                cmd = readSafeLine().trim().toLowerCase();
            } catch (InputCancelledException e) {

                System.out.println("Завершение авторизации.");
                return;
            }

            if (!cmd.equals("login") && !cmd.equals("register")) {
                System.out.println("Неверная команда. Доступно: login, register");
                continue;
            }

            try {
                if (processAuth(cmd)) return;
            } catch (InputCancelledException e) {
                System.out.println("Возврат к выбору команды.");
            }
        }
    }

    private boolean processAuth(String cmd) {
        while (true) {
            System.out.print("Логин: ");
            String login = readSafeLine().trim();
            if (login.isEmpty()) {
                System.out.println("Логин не может быть пустым");
                continue;
            }

            if (cmd.equals("register") && !checkLoginAvailability(login)) continue;

            while (true) {
                System.out.print("Пароль: ");
                String password = readSafeLine();
                if (password.isEmpty()) {
                    System.out.println("Пароль не может быть пустым");
                    continue;
                }

                try { checkPasswordComplexity(password); }
                catch (IllegalArgumentException e) { continue; }

                if (cmd.equals("register")) {
                    System.out.print("Повторите пароль: ");
                    String confirm = readSafeLine();
                    if (!password.equals(confirm)) {
                        System.out.println("Пароли не совпадают, попробуйте снова");
                        continue;
                    }
                }

                if (sendAuthRequest(cmd, login, password)) return true;

                if (lastAuthErrorMessage == null) {
                    System.out.println("Ошибка сети. Возврат к выбору команды.");
                    return false;
                }
                if (lastAuthErrorMessage.contains("уже существует") || lastAuthErrorMessage.contains("не найден")) {
                    System.out.println(lastAuthErrorMessage + ", введите другой логин");
                    break;
                }
                if (lastAuthErrorMessage.contains("Неверный пароль")) {
                    System.out.println("Неверный пароль, попробуйте снова");
                    continue;
                }
                System.out.println("Ошибка: " + lastAuthErrorMessage + ". Возврат к выбору команды.");
                return false;
            }
        }
    }

    private boolean checkLoginAvailability(String login) {
        try {
            Response check = client.sendRequest("register", new String[]{"check", login}, null);
            if (!check.isSuccess()) {
                System.out.println(check.getMessage());
                return false;
            }
            return true;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка сети при проверке логина");
            return false;
        }
    }

    private boolean sendAuthRequest(String cmd, String login, String password) {
        try {
            client.setCredentials(login, password);
            Response response = client.sendRequest(cmd, new String[0], null);
            if (response.isSuccess()) {
                System.out.println(response.getMessage());
                return true;
            }
            lastAuthErrorMessage = response.getMessage();
            return false;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка сети: " + e.getMessage());
            lastAuthErrorMessage = null;
            return false;
        }
    }

    private String readSafeLine() {
        String line = scanner.nextLine();
        if (inputStream.wasEofSignal()) {
            inputStream.clearEofSignal();
            System.out.println("\n[Ctrl+D]");
            throw new InputCancelledException();
        }
        return line;
    }

    private void checkPasswordComplexity(String password) {
        if (password.length() < 8 ||
                !password.matches(".*[A-ZА-ЯЁ].*") ||
                !password.matches(".*\\d.*") ||
                !password.matches(".*[^a-zA-Z0-9А-Яа-яёЁ].*")) {
            System.out.println("Пароль не соответствует требованиям: минимум 8 символов, заглавная буква, цифра, спецсимвол.");
            System.out.println("Попробуйте ввести пароль снова");
            throw new IllegalArgumentException();
        }
    }
}