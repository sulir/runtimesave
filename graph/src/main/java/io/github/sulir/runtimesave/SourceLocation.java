package io.github.sulir.runtimesave;

import com.sun.jdi.Location;
import org.jetbrains.annotations.NotNull;

public record SourceLocation(String className, String method, int line) {
    public static SourceLocation fromJDI(Location location) {
        String className = location.declaringType().name();
        String method = location.method().name() + location.method().signature();
        return new SourceLocation(className, method, location.lineNumber());
    }

    @SuppressWarnings("unused")
    public static SourceLocation fromJvmTi(String classSig, String methodName, String methodSig, int line) {
        String className = classSig.substring(1, classSig.length() - 1).replace('/', '.');
        return new SourceLocation(className, methodName + methodSig, line);
    }

    public @NotNull String toString() {
        return className + "." + method + ":" + line;
    }
}
