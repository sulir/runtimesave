package io.github.sulir.runtimesave.buffer;

import io.github.sulir.runtimesave.misc.SourceLocation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BufferReader {
    private final ByteBuffer buffer;

    public BufferReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public SourceLocation readLocation() {
        String classSig = readUTF8();
        String className = classSig.substring(1, classSig.length() - 1).replace('/', '.');
        String methodName = readUTF8();
        String methodSig = readUTF8();
        int line = buffer.getInt();
        return new SourceLocation(className, methodName + methodSig, line);
    }

    private String readUTF8() {
        int length = buffer.getInt();
        ByteBuffer view = buffer.slice(buffer.position(), length);
        String string = StandardCharsets.UTF_8.decode(view).toString();
        buffer.position(buffer.position() + length);
        return string;
    }
}
