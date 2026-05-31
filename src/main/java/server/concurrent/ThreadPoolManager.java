package server.concurrent;

import java.util.concurrent.ForkJoinPool;

/**
 * Менеджер пулов потоков для сервера.
 * Инкапсулирует создание, настройку и завершение ForkJoinPool.
 */
public class ThreadPoolManager {
    private final ForkJoinPool readPool;
    private final ForkJoinPool writePool;

    public ThreadPoolManager(int parallelism) {
        this.readPool = new ForkJoinPool(parallelism);
        this.writePool = new ForkJoinPool(parallelism);
    }

    public void submitRead(Runnable task) {
        readPool.submit(task);
    }

    public void submitWrite(Runnable task) {
        writePool.submit(task);
    }

    public ForkJoinPool getWritePool() {
        return writePool;
    }

    public void shutdown() {
        readPool.shutdown();
        writePool.shutdown();
    }
}