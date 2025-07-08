package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.NodeProperty;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.BiFunction;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class ObjectHasher {
    private final MessageDigest sha = uncheck(() -> MessageDigest.getInstance("SHA-224"));

    public ObjectHasher reset() {
        sha.reset();
        return this;
    }

    public ObjectHasher add(Object object) {
        return switch (object) {
            case Integer integer -> addInt(integer);
            case String string -> addString(string);
            case Enum<?> enumeration -> addEnum(enumeration);
            case NodeProperty[] properties -> addProperties(properties);
            case NodeHash hash -> addHash(hash);
            case Character character -> addFixed(Type.CHAR, character, Character.BYTES, ByteBuffer::putChar);
            case Byte aByte -> addFixed(Type.BYTE, aByte, Byte.BYTES, ByteBuffer::put);
            case Short aShort -> addFixed(Type.SHORT, aShort, Short.BYTES, ByteBuffer::putShort);
            case Long aLong -> addFixed(Type.LONG, aLong, Long.BYTES, ByteBuffer::putLong);
            case Float aFloat -> addFixed(Type.FLOAT, aFloat, Float.BYTES, ByteBuffer::putFloat);
            case Double aDouble -> addFixed(Type.DOUBLE, aDouble, Double.BYTES, ByteBuffer::putDouble);
            case Boolean aBoolean -> addFixed(Type.BOOLEAN, aBoolean, 1, (buf, o) -> buf.put((byte) (o ? 1 : 0)));
            default -> throw new IllegalArgumentException("Unsupported type: " + object.getClass().getName());
        };
    }

    public ObjectHasher addInt(int value) {
        return addTypeAndIntBytes(Type.INT, value);
    }

    public ObjectHasher addString(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        addTypeAndIntBytes(Type.STRING, bytes.length);
        sha.update(bytes);
        return this;
    }

    public ObjectHasher addEnum(Enum<?> enumeration) {
        return addTypeAndIntBytes(Type.ENUM, enumeration.ordinal());
    }

    public ObjectHasher addProperties(NodeProperty[] properties) {
        addTypeAndIntBytes(Type.PROPERTIES, properties.length);
        for (NodeProperty property : properties) {
            addString(property.key());
            add(property.value());
        }
        return this;
    }

    public ObjectHasher addHash(NodeHash hash) {
        byte[] bytes = hash.getBytes();
        return addFixed(Type.HASH, bytes, bytes.length, ByteBuffer::put);
    }

    public byte[] finish() {
        return sha.digest();
    }

    public byte[] hash(Object object) {
        reset();
        add(object);
        return finish();
    }

    private <T> ObjectHasher addFixed(Type type, T value, int size, BiFunction<ByteBuffer, T, ByteBuffer> putter) {
        ByteBuffer buffer = ByteBuffer.allocate(size + 1);
        buffer.put((byte) type.ordinal());
        putter.apply(buffer, value);
        sha.update(buffer.array());
        return this;
    }

    private ObjectHasher addTypeAndIntBytes(Type type, int i) {
        byte[] bytes = {(byte) type.ordinal(), (byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i};
        sha.update(bytes);
        return this;
    }

    private enum Type {
        INT, STRING, ENUM, PROPERTIES, HASH, CHAR, BYTE, SHORT, LONG, FLOAT, DOUBLE, BOOLEAN
    }
}
