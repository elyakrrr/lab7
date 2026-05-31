package client.io;

public class InputCancelledException extends RuntimeException {
    public InputCancelledException() {
        super("Ввод отменён (Ctrl+D)");
    }
}