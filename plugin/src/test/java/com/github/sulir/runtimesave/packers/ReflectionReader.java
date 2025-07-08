package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.*;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class ReflectionReader {
    private static final Unsafe unsafe;

    static {
        Field unsafeField = uncheck(() -> Unsafe.class.getDeclaredField("theUnsafe"));
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) uncheck(() -> unsafeField.get(null));
    }

    private final Map<Object, ValueNode> created = new IdentityHashMap<>();

    public ValueNode read(Object value) {
        return read(value, value == null ? Object.class : value.getClass());
    }

    public ValueNode read(Object value, Class<?> type) {
        if (value == null)
            return NullNode.getInstance();
        if (type.isPrimitive())
            return new PrimitiveNode(value, type.getName());

        ValueNode existing = created.get(value);
        if (existing != null)
            return existing;

        if (value instanceof String string) {
            StringNode stringNode = new StringNode(string);
            created.put(value, stringNode);
            return stringNode;
        }

        if (type.isArray()) {
            ArrayNode arrayNode = new ArrayNode(type.getName());
            created.put(value, arrayNode);
            int length = Array.getLength(value);
            Class<?> componentType = type.getComponentType();

            for (int i = 0; i < length; i++) {
                Object elem = Array.get(value, i);
                arrayNode.addElement(read(elem, componentType));
            }
            return arrayNode;
        }

        ObjectNode objectNode = new ObjectNode(type.getName());
        created.put(value, objectNode);

        for (Field field : findFields(type, new LinkedList<>())) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;

            @SuppressWarnings("deprecation")
            long offset = unsafe.objectFieldOffset(field);
            Object fieldValue = getFieldValue(value, offset, field.getType());
            ValueNode fieldNode = read(fieldValue, field.getType());
            objectNode.setField(field.getName(), fieldNode);
        }

        return objectNode;
    }

    private List<Field> findFields(Class<?> clazz, List<Field> result) {
        Collections.addAll(result, clazz.getDeclaredFields());
        if (clazz.getSuperclass() != null)
            findFields(clazz.getSuperclass(), result);
        return result;
    }

    private Object getFieldValue(Object object, long offset, Class<?> type) {
        if (type == char.class) return unsafe.getChar(object, offset);
        if (type == byte.class) return unsafe.getByte(object, offset);
        if (type == short.class) return unsafe.getShort(object, offset);
        if (type == int.class) return unsafe.getInt(object, offset);
        if (type == long.class) return unsafe.getLong(object, offset);
        if (type == float.class) return unsafe.getFloat(object, offset);
        if (type == double.class) return unsafe.getDouble(object, offset);
        if (type == boolean.class) return unsafe.getBoolean(object, offset);
        return unsafe.getObject(object, offset);
    }

    public static void main(String[] args) {
        ReflectionReader reader = new ReflectionReader();
        List<String> list = new LinkedList<>(List.of("Hello", "World"));
        ValueNode node = reader.read(list);
        System.out.println(node);
    }
}
