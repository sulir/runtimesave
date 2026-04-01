package io.github.sulir.runtimesave.misc;

import java.io.PrintStream;
import java.util.function.Supplier;

public class Log {
    private enum Level {
        OFF, ERROR, INFO, DEBUG
    }

    private static final Level level = switch(System.getenv("RS_LOG")) {
        case "off" -> Level.OFF;
        case "info" -> Level.INFO;
        case "debug" -> Level.DEBUG;
        case null, default -> Level.ERROR;
    };
    private static final PrintStream output = System.err;

    public static void error(String message) {
        if (level.ordinal() >= Level.ERROR.ordinal())
            output.println("ERROR: " + message);
    }

    public static void error(Throwable throwable) {
        if (level.ordinal() >= Level.ERROR.ordinal())
            throwable.printStackTrace(output);
    }

    public static void info(Object text) {
        if (level.ordinal() >= Level.INFO.ordinal())
            output.println(text.toString());
    }

    public static void debug(Supplier<?> text) {
        if (level.ordinal() >= Level.DEBUG.ordinal())
            output.println(text.get().toString());
    }
}
