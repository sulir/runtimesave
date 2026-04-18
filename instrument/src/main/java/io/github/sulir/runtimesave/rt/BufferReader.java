package io.github.sulir.runtimesave.rt;

import io.github.sulir.runtimesave.graph.ValueNode;
import io.github.sulir.runtimesave.misc.SourceLocation;
import io.github.sulir.runtimesave.nodes.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class BufferReader implements AutoCloseable {
    private static ClassInfo[] classesInfo = new ClassInfo[16 * 1024];
    private static final AtomicLong lastSequence = new AtomicLong(0);

    private final long sequenceNum;
    private final int referenceNodeCount;
    private final ByteBuffer main;
    private final ByteBuffer location;
    private final ByteBuffer locals;
    private final ByteBuffer heap;
    private final ByteBuffer classes;
    private boolean classesRead = false;

    public BufferReader(ByteBuffer main) {
        this.main = main;
        main.order(ByteOrder.LITTLE_ENDIAN);

        sequenceNum = main.getLong();
        referenceNodeCount = main.getInt();
        int locationStart = main.getInt();
        int localsStart = main.getInt();
        int heapStart = main.getInt();
        int classesStart = main.getInt();

        location = main.slice(locationStart, localsStart - locationStart);
        locals = main.slice(localsStart, heapStart - localsStart);
        heap = main.slice(heapStart, classesStart - heapStart);
        classes = main.slice(classesStart, main.limit() - classesStart);

        for (ByteBuffer buffer : new ByteBuffer[]{location, locals, heap, classes})
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
        List<String> referenceLocals = new ArrayList<>();

        while (locals.hasRemaining()) {
            String variableName = readUTF8(locals);
            byte kind = locals.get();
            if (kind == 'R')
                referenceLocals.add(variableName);
            else if (kind == 'N')
                frame.setVariable(variableName, NullNode.getInstance());
            else
                frame.setVariable(variableName, readPrimitive(kind, locals));
        }

        readHeap(frame, referenceLocals);
        return frame;
    }

    private void readHeap(FrameNode frame, List<String> referenceLocals) {
        Map<Long, ValueNode> nodes = new HashMap<>(referenceNodeCount);

        while (heap.hasRemaining()) {
            byte kind = heap.get();
            switch (kind) {
                case 'R' -> readObjectOrArray(nodes);
                case 'T' -> readString(nodes);
                case 'M' -> readFieldEdge(nodes);
                case 'E' -> readElementEdge(nodes, frame, referenceLocals);
                default -> {
                    if (kind >= 'B' && kind <= 'Z')
                        readPrimitiveField(kind, nodes);
                    else if (kind >= 'b' && kind <= 'z')
                        readPrimitiveArray((byte) (kind - 'a' + 'A'), nodes);
                }
            }
        }
    }

    private void readObjectOrArray(Map<Long, ValueNode> nodes) {
        long objectTag = heap.getLong();
        int classTag = heap.getInt();

        if (classesInfo[classTag] == null)
            System.err.println("NULL: " + classTag);

        String type = classesInfo[classTag].className();
        ValueNode node = nodes.get(objectTag);
        switch (node) {
            case ObjectNode object -> object.setType(type);
            case ArrayNode array -> array.setType(type);
            case null, default -> throw new IllegalStateException();
        }
    }

    private void readString(Map<Long, ValueNode> nodes) {
        long objectTag = heap.getLong();
        String value = readUTF16(heap);

        ((StringNode) nodes.get(objectTag)).setValue(value);
    }

    private void readFieldEdge(Map<Long, ValueNode> nodes) {
        long from = heap.getLong();
        int fromClass = heap.getInt();
        int fieldIndex = heap.getInt();
        long to = heap.getLong();
        byte toKind = heap.get();

        ObjectNode source = (ObjectNode) nodes.get(from);
        ValueNode target = getOrCreateNode(nodes, to, toKind);
        setField(source, fromClass, fieldIndex, target);
    }

    private void readElementEdge(Map<Long, ValueNode> nodes, FrameNode frame, List<String> referenceLocals) {
        long from = heap.getLong();
        int index = heap.getInt();
        long to = heap.getLong();
        byte toKind = heap.get();

        ArrayNode source = (ArrayNode) nodes.get(from);
        ValueNode target = getOrCreateNode(nodes, to, toKind);

        if (source == null) {
            if (from != 0)
                throw new IllegalStateException();
            frame.setVariable(referenceLocals.get(index), target);
        } else {
            source.setElement(index, target);
        }
    }

    private void readPrimitiveField(byte type, Map<Long, ValueNode> nodes) {
        long objectTag = heap.getLong();
        int classTag = heap.getInt();
        int fieldIndex = heap.getInt();
        ValueNode value = readPrimitive(type, heap);

        ObjectNode object = (ObjectNode) nodes.get(objectTag);
        setField(object, classTag, fieldIndex, value);
    }

    private void readPrimitiveArray(byte type, Map<Long, ValueNode> nodes) {
        long objectTag = heap.getLong();
        int length = heap.getInt();

        ArrayNode array = (ArrayNode) nodes.get(objectTag);
        for (int i = 0; i < length; i++)
            array.setElement(i, readPrimitive(type, heap));
    }

    private static ValueNode getOrCreateNode(Map<Long, ValueNode> nodes, long tag, byte kind) {
        return nodes.computeIfAbsent(tag, (t) -> switch (kind) {
            case 'T' -> new StringNode();
            case '[' -> new ArrayNode();
            case 'L' -> new ObjectNode();
            default -> throw new IllegalArgumentException("Unknown target kind: " + (char) kind);
        });
    }

    private void setField(ObjectNode object, int classTag, int fieldIndex, ValueNode value) {
        ClassInfo info = classesInfo[classTag];
        String fieldName = info.fieldNames()[fieldIndex - info.fieldStartIndex()];
        object.setField(fieldName, value);
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

    private static String readUTF16(ByteBuffer buffer) {
        int charCount = buffer.getInt();
        char[] chars = new char[charCount];
        buffer.asCharBuffer().get(chars);
        String result = new String(chars);
        buffer.position(buffer.position() + 2 * charCount);
        return result;
    }

    private static PrimitiveNode readPrimitive(byte type, ByteBuffer buffer) {
        return switch (type) {
            case 'Z' -> new PrimitiveNode(buffer.get() != 0, "boolean");
            case 'B' -> new PrimitiveNode(buffer.get(), "byte");
            case 'C' -> new PrimitiveNode(buffer.getChar(), "char");
            case 'S' -> new PrimitiveNode(buffer.getShort(), "short");
            case 'I' -> new PrimitiveNode(buffer.getInt(), "int");
            case 'J' -> new PrimitiveNode(buffer.getLong(), "long");
            case 'F' -> new PrimitiveNode(buffer.getFloat(), "float");
            case 'D' -> new PrimitiveNode(buffer.getDouble(), "double");
            default -> throw new IllegalArgumentException("Unknown primitive type: " + (char) type);
        };
    }

    @Override
    public void close() {
        dispose(main);
    }

    private static native void dispose(ByteBuffer buffer);
}
