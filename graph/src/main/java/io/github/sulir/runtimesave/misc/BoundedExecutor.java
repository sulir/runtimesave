package io.github.sulir.runtimesave.misc;

import java.util.concurrent.*;

public class BoundedExecutor extends ThreadPoolExecutor {
    public static final String PREFIX = "RuntimeSave";
    private static final int DEFAULT_QUEUE_SIZE = 10;

    public static BoundedExecutor forPartOfCores(double part, String name, BoundedExecutor... dependencies) {
        int cores = (int) Math.round(part * Runtime.getRuntime().availableProcessors());
        return new BoundedExecutor(cores, DEFAULT_QUEUE_SIZE, name, dependencies);
    }

    public BoundedExecutor(int threads, int queueSize, String name, BoundedExecutor... dependencies) {
        super(threads, threads, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize),
                Thread.ofPlatform().name(PREFIX + name +  "-", 1).daemon().factory());
        setRejectedExecutionHandler(this::rejectedExecution);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> appShuttingDown(dependencies)));
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
