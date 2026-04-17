package io.github.sulir.runtimesave.misc;

import java.util.concurrent.atomic.LongAdder;

@SuppressWarnings("unused")
public final class ScopeTime implements AutoCloseable {
    private static final int MAX_TIMERS = 32;
    private static final LongAdder[] totals = new LongAdder[MAX_TIMERS];
    private static final LongAdder[] counts = new LongAdder[MAX_TIMERS];

    static {
        for (int i = 0; i < MAX_TIMERS; i++) {
            totals[i] = new LongAdder();
            counts[i] = new LongAdder();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = 0; i < MAX_TIMERS; i++) {
                long nanos = totals[i].sum();
                long count = counts[i].sum();
                if (nanos != 0) {
                    System.err.printf("Java timer %d: %d ms, %dx%n", i, nanos / 1_000_000, count);
                }
            }
        }));
    }

    private final int id;
    private final long startNanos;

    public ScopeTime() {
        this(0);
    }

    public ScopeTime(int id) {
        if (id < 0 || id >= MAX_TIMERS) {
            throw new IllegalArgumentException("Timer ID out of range: " + id);
        }
        this.id = id;
        this.startNanos = System.nanoTime();
    }

    @Override
    public void close() {
        long diff = System.nanoTime() - startNanos;
        totals[id].add(diff);
        counts[id].increment();
    }
}