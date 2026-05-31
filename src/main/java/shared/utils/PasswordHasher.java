package shared.utils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHasher {
    private static final String ALGORITHM = "SHA-224";

    /**
     * Хэширует пароль алгоритмом SHA-224.
     * @return hex-строка хэша
     */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-224 алгоритм недоступен", e);
        }
    }

    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        return hash(password).equalsIgnoreCase(storedHash);
    }
}