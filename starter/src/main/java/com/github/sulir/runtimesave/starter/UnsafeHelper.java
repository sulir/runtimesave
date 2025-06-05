package com.github.sulir.runtimesave.starter;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;

@SuppressWarnings("unused")
public class UnsafeHelper {
    private static final Map<String, Class<?>> primitives = Map.of("char", char.class,
            "byte", byte.class, "short", short.class, "int", int.class, "long", long.class,
            "float", float.class, "double", double.class, "boolean", boolean.class);
    private static final Unsafe unsafe = getUnsafe();

    public static void ensureLoadedForJdi() { }

    public static Object allocateInstance(String className) {
        try {
            return unsafe.allocateInstance(Class.forName(className));
        } catch (InstantiationException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object allocateArray(String type, int length) {
        try {
            String component = type.substring(0, type.indexOf("["));
            int dimensions = (int) type.chars().filter(c -> c == '[').count();

            Class<?> componentType = primitives.get(component);
            if (componentType == null)
                componentType = Class.forName(component);

            for (int i = 0; i < dimensions - 1; i++)
                componentType = componentType.arrayType();

            return Array.newInstance(componentType, length);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static long getOffset(Object object, String fieldName) {
        Field field = findField(object.getClass(), fieldName);
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

    private static Unsafe getUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return ((Unsafe) unsafeField.get(null));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() == null)
                throw new RuntimeException(e);
            return findField(clazz.getSuperclass(), name);
        }
    }
}
