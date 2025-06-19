package com.github.sulir.runtimesave.hash;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
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
    private final int shaBytes = sha.getDigestLength();

    public void add(Object object) {
        if (object == null)
            addNull();
        else if (object instanceof String string)
            addString(string);
        else if (object instanceof Collection<?> collection)
            addCollection(collection);
        else if (object instanceof SortedMap<?, ?> map)
            addMap(map);
        else if (object instanceof byte[] hash && hash.length == shaBytes)
            addHash(hash);
        else
            addPrimitive(object);
    }

    public void addPrimitive(Object primitive) {
        Primitive properties = primitiveProperties.get(primitive.getClass());
        if (properties == null)
            throw new IllegalArgumentException("Unsupported type: " + primitive.getClass().getName());

        addType(properties.type());
        sha.update(properties.putValue().apply(allocate(properties.size()), primitive).array());
    }

    public void addNull() {
        addType(Type.NULL);
    }

    public void addString(String string) {
        addType(Type.STRING);
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        addLength(bytes.length);
        sha.update(bytes);
    }

    public void addCollection(Collection<?> collection) {
        addType(Type.COLLECTION);
        addLength(collection.size());
        collection.forEach(this::add);
    }

    public void addMap(SortedMap<?, ?> map) {
        addType(Type.MAP);
        addLength(map.size());
        map.forEach((key, value) -> {
            add(key);
            add(value);
        });
    }

    public void addHash(byte[] hash) {
        addType(Type.HASH);
        sha.update(hash);
    }

    public byte[] hash() {
        return sha.digest();
    }

    public byte[] hash(Object object) {
        add(object);
        return hash();
    }

    private void addType(Type type) {
        sha.update((byte) type.ordinal());
    }

    private void addLength(int length) {
        sha.update(allocate(Integer.BYTES).putInt(length).array());
    }

    private record Primitive(Type type, int size, BiFunction<ByteBuffer, Object, ByteBuffer> putValue) { }

    private enum Type {
        CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN, NULL, STRING, COLLECTION, MAP, HASH
    }
}
