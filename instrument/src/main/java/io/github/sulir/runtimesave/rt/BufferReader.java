package io.github.sulir.runtimesave.rt;

import io.github.sulir.runtimesave.graph.ValueNode;
import io.github.sulir.runtimesave.misc.SourceLocation;
import io.github.sulir.runtimesave.nodes.FrameNode;
import io.github.sulir.runtimesave.nodes.NullNode;
import io.github.sulir.runtimesave.nodes.ObjectNode;
import io.github.sulir.runtimesave.nodes.PrimitiveNode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class BufferReader {
    private final ByteBuffer buffer;

    public BufferReader(ByteBuffer buffer) {
        this.buffer = buffer;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public SourceLocation readLocation() {
        buffer.rewind();

        String classSig = readUTF8();
        String className = classSig.substring(1, classSig.length() - 1).replace('/', '.');
        String methodName = readUTF8();
        String methodSig = readUTF8();
        int line = buffer.getInt();
        return new SourceLocation(className, methodName + methodSig, line);
    }

    public FrameNode readFrame() {
        if (buffer.position() == 0)
            readLocation();

        FrameNode frame = new FrameNode();
        while (buffer.hasRemaining()) {
            String variableName = readUTF8();
            frame.setVariable(variableName, readVariable());
        }
        return frame;
    }

    private ValueNode readVariable() {
        byte kind = buffer.get();
        return switch (kind) {
            case 'Z' -> new PrimitiveNode(buffer.get() != 0, "boolean");
            case 'B' -> new PrimitiveNode(buffer.get(), "byte");
            case 'C' -> new PrimitiveNode(buffer.getChar(), "char");
            case 'S' -> new PrimitiveNode(buffer.getShort(), "short");
            case 'I' -> new PrimitiveNode(buffer.getInt(), "int");
            case 'J' -> new PrimitiveNode(buffer.getLong(), "long");
            case 'F' -> new PrimitiveNode(buffer.getFloat(), "float");
            case 'D' -> new PrimitiveNode(buffer.getDouble(), "double");
            case 'R' -> {
                buffer.getInt();
                yield new ObjectNode("Unknown");
            }
            case 'N' -> NullNode.getInstance();
            default -> throw new IllegalArgumentException("Unknown variable kind: " + (char) kind);
        };
    }

    private String readUTF8() {
        int length = buffer.getInt();
        ByteBuffer view = buffer.slice(buffer.position(), length);
        String string = StandardCharsets.UTF_8.decode(view).toString();
        buffer.position(buffer.position() + length);
        return string;
    }

    public void close() {
        dispose(buffer);
    }

    private static native void dispose(ByteBuffer buffer);
}
