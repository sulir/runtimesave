package io.github.sulir.runtimesave.misc;

import java.util.concurrent.*;

public class BoundedExecutor extends ThreadPoolExecutor {
    public static final String PREFIX = "RuntimeSave";
    private static final int DEFAULT_QUEUE_SIZE = 10;

    public static BoundedExecutor singleThreaded(String name, BoundedExecutor... dependencies) {
        return new BoundedExecutor(1, DEFAULT_QUEUE_SIZE, name, dependencies);
    }

    public static BoundedExecutor usingAllCores(String name, BoundedExecutor... dependencies) {
        return new BoundedExecutor(Runtime.getRuntime().availableProcessors(), DEFAULT_QUEUE_SIZE, name, dependencies);
    }

    public BoundedExecutor(int threads, int queueSize, String name, BoundedExecutor... dependencies) {
        super(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize),
                Thread.ofPlatform().name(PREFIX + name +  "-", 1).daemon().factory());
        setRejectedExecutionHandler(this::rejectedExecution);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> appShuttingDown(dependencies)));
    }

    private void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            Log.error("Task rejected during shutdown");
            return;
        }

        try {
            getQueue().put(runnable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(e);
        }
    }

    private void appShuttingDown(BoundedExecutor[] dependencies) {
        for (BoundedExecutor dependency : dependencies) {
            try {
                boolean ignored = dependency.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Log.error(e);
                dependency.shutdownNow();
            }
        }

        shutdown();
        try {
            boolean ignored = awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Log.error(e);
            shutdownNow();
        }
    }
}
