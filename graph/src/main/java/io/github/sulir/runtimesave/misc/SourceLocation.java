package io.github.sulir.runtimesave.misc;

import org.jetbrains.annotations.NotNull;

public record SourceLocation(String className, String method, int line) {
    public @NotNull String toString() {
        return className + "." + method + ":" + line;
    }
}
