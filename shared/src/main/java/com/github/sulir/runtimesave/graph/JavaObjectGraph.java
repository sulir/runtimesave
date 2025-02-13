package com.github.sulir.runtimesave.graph;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class JavaObjectGraph {
    private final GraphNode root;
    private final Map<ObjectNode, Object> visited = new HashMap<>();

    public JavaObjectGraph(GraphNode root) {
        this.root = root;
    }

    public Object create() {
        Object result = transform(root);
        visited.clear();
        return result;
    }

    private Object transform(GraphNode node) {
        if (node instanceof PrimitiveNode primitive) {
            return primitive.getValue();
        } else if (node instanceof NullNode) {
            return null;
        } else if (node instanceof StringNode string) {
            return string.getValue();
        } else if (node instanceof ObjectNode objectNode) {
            Object existing = visited.get(objectNode);
            if (existing != null)
                return existing;

            Object object = initializeMemory(objectNode);
            visited.put(objectNode, object);

            for (var entry : objectNode.getFields().entrySet()) {
                String name = entry.getKey();
                GraphNode fieldNode = entry.getValue();
                Object value = transform(fieldNode);
                setFieldValue(object, name, value);
            }

            return object;
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node.getClass());
        }
    }

    private Object initializeMemory(ObjectNode node) {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            return ((Unsafe) unsafe.get(null)).allocateInstance(Class.forName(node.getType()));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setFieldValue(Object object, String name, Object value) {
        Field field = findField(object.getClass(), name);
        field.setAccessible(true);
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() == null)
                throw new RuntimeException(e);
            return findField(clazz.getSuperclass(), name);
        }
    }
}
