package net.sadness.maintenance.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

class AsyncWriterTest {

    private static final int TASK_COUNT = 200;
    private static final long DELAY_NANOS = 50_000_000L;

    @Test
    void runsTasksInSubmissionOrder() {
        List<Integer> order = new CopyOnWriteArrayList<>();
        try (AsyncWriter writer = new AsyncWriter()) {
            for (int i = 0; i < TASK_COUNT; i++) {
                int value = i;
                writer.submit(() -> order.add(value), e -> { });
            }
        }
        assertEquals(TASK_COUNT, order.size());
        for (int i = 0; i < TASK_COUNT; i++) {
            assertEquals(i, order.get(i));
        }
    }

    @Test
    void runsTasksOffTheCallingThread() {
        AtomicReference<Thread> worker = new AtomicReference<>();
        try (AsyncWriter writer = new AsyncWriter()) {
            writer.submit(() -> worker.set(Thread.currentThread()), e -> { });
        }
        assertNotEquals(Thread.currentThread(), worker.get());
        assertTrue(worker.get().isDaemon());
    }

    @Test
    void reportsFailuresAndKeepsProcessing() {
        AtomicReference<IOException> failure = new AtomicReference<>();
        AtomicReference<String> later = new AtomicReference<>();
        try (AsyncWriter writer = new AsyncWriter()) {
            writer.submit(() -> {
                throw new IOException("disk full");
            }, failure::set);
            writer.submit(() -> later.set("done"), e -> { });
        }
        assertEquals("disk full", failure.get().getMessage());
        assertEquals("done", later.get());
    }

    @Test
    void closeFlushesQueuedWork() {
        AtomicReference<String> value = new AtomicReference<>();
        AsyncWriter writer = new AsyncWriter();
        writer.submit(() -> {
            LockSupport.parkNanos(DELAY_NANOS);
            value.set("written");
        }, e -> { });
        writer.close();
        assertEquals("written", value.get());
    }

    @Test
    void submitAfterCloseIsRejected() {
        AsyncWriter writer = new AsyncWriter();
        writer.close();
        assertFalse(writer.submit(() -> { }, e -> { }));
    }
}
