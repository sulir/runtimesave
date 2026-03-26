package io.github.sulir.runtimesave.misc;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BoundedExecutor extends ThreadPoolExecutor {
    private static final int DEFAULT_QUEUE_SIZE = 10;

    public static BoundedExecutor singleThreaded() {
        return new BoundedExecutor(1, DEFAULT_QUEUE_SIZE);
    }

    public static BoundedExecutor usingAllCores() {
        return new BoundedExecutor(Runtime.getRuntime().availableProcessors(), DEFAULT_QUEUE_SIZE);
    }

    public BoundedExecutor(int threads, int queueSize) {
        super(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize));
        setRejectedExecutionHandler(this::rejectedExecution);
        Runtime.getRuntime().addShutdownHook(new Thread(this::appShuttingDown));
    }

    private void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown())
            return;

        try {
            getQueue().put(runnable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(e);
        }
    }

    private void appShuttingDown() {
        shutdown();
        try {
            if (!awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
                shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
            shutdownNow();
        }
    }
}
