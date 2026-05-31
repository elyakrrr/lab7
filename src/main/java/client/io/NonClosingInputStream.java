package client.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Обёртка над InputStream, которая перехватывает EOF (Ctrl+D на Linux)
 * и не закрывает поток. Вместо -1 возвращает '\n', чтобы Scanner
 * завершил текущую строку и не завис. После этого можно проверить
 * wasEofSignal() и бросить InputCancelledException.
 */
public class NonClosingInputStream extends InputStream {

    private final InputStream wrapped;

    private volatile boolean eofSignal = false;

    public NonClosingInputStream(InputStream wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        int b = wrapped.read();
        if (b == -1) {
            eofSignal = true;
            return '\n';
        }
        return b;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public int read(byte[] buf, int off, int len) throws IOException {
        int result = wrapped.read(buf, off, len);
        if (result == -1) {
            eofSignal = true;
            buf[off] = '\n';
            return 1;
        }

        return result;
    }

    /**
     * Был ли сигнал EOF с момента последнего вызова clearEofSignal().
     */
    public boolean wasEofSignal() {
        return eofSignal;
    }

    /**
     * Сбросить флаг EOF — вызывать после обработки Ctrl+D.
     */
    public void clearEofSignal() {
        eofSignal = false;
    }
}