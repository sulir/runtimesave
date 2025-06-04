package com.github.sulir.runtimesave.starter;

import com.github.sulir.runtimesave.graph.GraphLoader;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeHelper {
    private static final Unsafe unsafe = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return ((Unsafe) unsafeField.get(null));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void ensureLoadedForJdi() { }

    public static Object allocateInstance(String className) {
        try {
            return unsafe.allocateInstance(Class.forName(className));
        } catch (InstantiationException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object allocateArray(String arrayClass, int length) {
        return GraphLoader.allocateArray(arrayClass, length);
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static long getOffset(Object object, String fieldName) {
        Field field = GraphLoader.findField(object.getClass(), fieldName);
        return unsafe.objectFieldOffset(field);
    }

    public static void putValue(Object object, String fieldName, char value) {
        unsafe.putChar(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, byte value) {
        unsafe.putByte(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, short value) {
        unsafe.putShort(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, int value) {
        unsafe.putInt(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, long value) {
        unsafe.putLong(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, float value) {
        unsafe.putFloat(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, double value) {
        unsafe.putDouble(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, boolean value) {
        unsafe.putBoolean(object, getOffset(object, fieldName), value);
    }

    public static void putValue(Object object, String fieldName, Object value) {
        unsafe.putObject(object, getOffset(object, fieldName), value);
    }
}
