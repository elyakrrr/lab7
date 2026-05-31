package shared.model;
import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final String username;
    private final String passwordHash;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getPasswordHash() { return passwordHash; }

    @Override
    public String toString() {
        return "User{username='" + username + "'}";
    }
}