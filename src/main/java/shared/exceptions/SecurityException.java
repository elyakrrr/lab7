package shared.exceptions;
import java.io.Serial;
import java.io.Serializable;

public class SecurityException extends Exception implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public SecurityException(String message) {
        super(message);
    }
}