package net.sadness.maintenance.io;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class AsyncWriter implements AutoCloseable {

    @FunctionalInterface
    public interface IoAction {
        void run() throws IOException;
    }

    private static final String THREAD_NAME = "maintenance-io";
    private static final long CLOSE_TIMEOUT_SECONDS = 10;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, THREAD_NAME);
        thread.setDaemon(true);
        return thread;
    });

    public boolean submit(IoAction action, Consumer<IOException> onFailure) {
        try {
            executor.execute(() -> {
                try {
                    action.run();
                } catch (IOException e) {
                    onFailure.accept(e);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
