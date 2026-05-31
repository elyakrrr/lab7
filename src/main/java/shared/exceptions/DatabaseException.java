package shared.exceptions;
import java.io.Serial;
import java.io.Serializable;

public class DatabaseException extends Exception implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
