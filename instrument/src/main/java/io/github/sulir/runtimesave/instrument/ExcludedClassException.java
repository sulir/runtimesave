package io.github.sulir.runtimesave.instrument;

public class ExcludedClassException extends RuntimeException {
    public static final ExcludedClassException INSTANCE = new ExcludedClassException();

    private ExcludedClassException() {
        super(null, null, false, false);
    }
}
