package com.github.sulir.runtimesave;

public interface UncheckedThrowing<T> {
    T call() throws Throwable;

    static <T> T uncheck(UncheckedThrowing<T> callable) {
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
