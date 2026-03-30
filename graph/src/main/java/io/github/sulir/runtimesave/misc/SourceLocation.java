package io.github.sulir.runtimesave.misc;

import com.sun.jdi.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record SourceLocation(String className, String method, int line) {
    public static SourceLocation fromJDI(Location location) {
        String className = location.declaringType().name();
        String method = location.method().name() + location.method().signature();
        return new SourceLocation(className, method, location.lineNumber());
    }

    public static SourceLocation fromBuffer(ByteBuffer buffer) {
        return fromJvmTi(readUTF8(buffer), readUTF8(buffer), readUTF8(buffer), buffer.getInt());
    }

    private static SourceLocation fromJvmTi(String classSig, String methodName, String methodSig, int line) {
        String className = classSig.substring(1, classSig.length() - 1).replace('/', '.');
        return new SourceLocation(className, methodName + methodSig, line);
    }

    private static String readUTF8(ByteBuffer buffer) {
        int length = buffer.getInt();
        ByteBuffer view = buffer.slice(buffer.position(), length);
        String string = StandardCharsets.UTF_8.decode(view).toString();
        buffer.position(buffer.position() + length);
        return string;
    }

    public @NotNull String toString() {
        return className + "." + method + ":" + line;
    }
}
