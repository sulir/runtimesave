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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class BufferReader {
    private static ClassInfo[] classesInfo = new ClassInfo[16 * 1024];
    private static final AtomicLong lastSequence = new AtomicLong(0);

    private final long sequenceNum;
    private final ByteBuffer main;
    private final ByteBuffer location;
    private final ByteBuffer locals;
    private final ByteBuffer classes;
    private boolean classesRead = false;

    public BufferReader(ByteBuffer main) {
        this.main = main;
        main.order(ByteOrder.LITTLE_ENDIAN);

        sequenceNum = main.getLong();
        int locationStart = main.getInt();
        int localsStart = main.getInt();
        int nodesStart = main.getInt();
        int classesStart = main.getInt();

        location = main.slice(locationStart, localsStart - locationStart);
        locals = main.slice(localsStart, nodesStart - localsStart);
        classes = main.slice(classesStart, main.limit() - classesStart);

        for (ByteBuffer buffer : new ByteBuffer[]{location, locals, classes})
            buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public SourceLocation readLocation() {
        String classSig = readUTF8(location);
        String className = classSig.substring(1, classSig.length() - 1).replace('/', '.');
        String methodName = readUTF8(location);
        String methodSig = readUTF8(location);
        int line = location.getInt();
        return new SourceLocation(className, methodName + methodSig, line);
    }

    public FrameNode readFrame() {
        if (!classesRead)
            throw new IllegalStateException("Metadata of classes not yet read");

        FrameNode frame = new FrameNode();
        while (locals.hasRemaining()) {
            String variableName = readUTF8(locals);
            frame.setVariable(variableName, readVariable());
        }
        return frame;
    }

    private ValueNode readVariable() {
        byte kind = locals.get();
        return switch (kind) {
            case 'Z' -> new PrimitiveNode(locals.get() != 0, "boolean");
            case 'B' -> new PrimitiveNode(locals.get(), "byte");
            case 'C' -> new PrimitiveNode(locals.getChar(), "char");
            case 'S' -> new PrimitiveNode(locals.getShort(), "short");
            case 'I' -> new PrimitiveNode(locals.getInt(), "int");
            case 'J' -> new PrimitiveNode(locals.getLong(), "long");
            case 'F' -> new PrimitiveNode(locals.getFloat(), "float");
            case 'D' -> new PrimitiveNode(locals.getDouble(), "double");
            case 'R' -> new ObjectNode("Unknown");
            case 'N' -> NullNode.getInstance();
            default -> throw new IllegalArgumentException("Unknown variable kind: " + (char) kind);
        };
    }

    public void readClasses() {
        while (classes.hasRemaining()) {
            int tag = classes.getInt();
            if (tag >= classesInfo.length)
                classesInfo = Arrays.copyOf(classesInfo, Math.max(2 * classesInfo.length, tag + 1));
            classesInfo[tag] = readClassInfo();
        }
        waitForPreviousClasses();
    }

    private ClassInfo readClassInfo() {
        String className = BufferReader.readUTF8(classes);
        int fieldStartIndex = classes.getInt();
        int fieldCount = classes.getInt();
        String[] fieldNames = new String[fieldCount];
        for (int i = 0; i < fieldCount; i++)
            fieldNames[i] = BufferReader.readUTF8(classes);

        return new ClassInfo(className, fieldStartIndex, fieldNames);
    }

    private void waitForPreviousClasses() {
        while (!lastSequence.compareAndSet(sequenceNum - 1, sequenceNum))
            Thread.onSpinWait();
        classesRead = true;
    }

    public static String readUTF8(ByteBuffer buffer) {
        int length = buffer.getInt();
        ByteBuffer view = buffer.slice(buffer.position(), length);
        String string = StandardCharsets.UTF_8.decode(view).toString();
        buffer.position(buffer.position() + length);
        return string;
    }

    public void close() {
        dispose(main);
    }

    private static native void dispose(ByteBuffer buffer);
}
