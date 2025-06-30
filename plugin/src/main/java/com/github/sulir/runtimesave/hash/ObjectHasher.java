package com.github.sulir.runtimesave.hash;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiFunction;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;
import static java.nio.ByteBuffer.allocate;

public class ObjectHasher {
    private static final Map<Class<?>, Primitive> primitiveProperties = Map.of(
            Character.class, new Primitive(Type.CHAR, Character.BYTES, (buf, o) -> buf.putChar((char) o)),
            Byte.class, new Primitive(Type.BYTE, Byte.BYTES, (buf, o) -> buf.put((byte) o)),
            Short.class, new Primitive(Type.SHORT, Short.BYTES, (buf, o) -> buf.putShort((short) o)),
            Integer.class, new Primitive(Type.INT, Integer.BYTES, (buf, o) -> buf.putInt((int) o)),
            Long.class, new Primitive(Type.LONG, Long.BYTES, (buf, o) -> buf.putLong((long) o)),
            Float.class, new Primitive(Type.FLOAT, Float.BYTES, (buf, o) -> buf.putFloat((float) o)),
            Double.class, new Primitive(Type.DOUBLE, Double.BYTES, (buf, o) -> buf.putDouble((double) o)),
            Boolean.class, new Primitive(Type.BOOLEAN, 1, (buf, o) -> buf.put((byte) ((boolean) o ? 1 : 0)))
    );

    private final MessageDigest sha = uncheck(() -> MessageDigest.getInstance("SHA-224"));

    public ObjectHasher reset() {
        sha.reset();
        return this;
    }

    public ObjectHasher add(Object object) {
        if (object == null)
            return addNull();
        if (object instanceof String string)
            return addString(string);
        if (object instanceof List<?> list)
            return addList(list);
        if (object instanceof SortedMap<?, ?> map)
            return addMap(map);
        if (object instanceof NodeHash hash)
            return addHash(hash);
        return addPrimitive(object);
    }

    public ObjectHasher addPrimitive(Object primitive) {
        Primitive properties = primitiveProperties.get(primitive.getClass());
        if (properties == null)
            throw new IllegalArgumentException("Unsupported type: " + primitive.getClass().getName());

        addType(properties.type());
        sha.update(properties.putValue().apply(allocate(properties.size()), primitive).array());
        return this;
    }

    public ObjectHasher addNull() {
        addType(Type.NULL);
        return this;
    }

    public ObjectHasher addString(String string) {
        addType(Type.STRING);
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        addLength(bytes.length);
        sha.update(bytes);
        return this;
    }

    public ObjectHasher addList(List<?> list) {
        addType(Type.LIST);
        addLength(list.size());
        list.forEach(this::add);
        return this;
    }

    public ObjectHasher addMap(SortedMap<?, ?> map) {
        addType(Type.MAP);
        addLength(map.size());
        map.forEach((key, value) -> {
            add(key);
            add(value);
        });
        return this;
    }

    public ObjectHasher addHash(NodeHash hash) {
        addType(Type.HASH);
        sha.update(hash.getBytes());
        return this;
    }

    public byte[] finish() {
        return sha.digest();
    }

    public byte[] hash(Object object) {
        reset();
        add(object);
        return finish();
    }

    private void addType(Type type) {
        sha.update((byte) type.ordinal());
    }

    private void addLength(int length) {
        sha.update(allocate(Integer.BYTES).putInt(length).array());
    }

    private record Primitive(Type type, int size, BiFunction<ByteBuffer, Object, ByteBuffer> putValue) { }

    private enum Type {
        CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL, STRING, LIST, MAP, HASH
    }
}
