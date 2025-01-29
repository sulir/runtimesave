package com.github.sulir.runtimesave.graph;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class JavaObjectGraph {
    private final LazyObjectGraph lazyGraph;
    private final Map<ObjectNode, Object> visited = new HashMap<>();

    public JavaObjectGraph(LazyObjectGraph lazyGraph) {
        this.lazyGraph = lazyGraph;
    }

    public Object create() {
        LazyNode root = lazyGraph.getRoot();
        Object result = transform(root);
        visited.clear();
        return result;
    }

    private Object transform(LazyNode node) {
        if (node instanceof PrimitiveNode primitive) {
            return primitive.getValue();
        } else if (node instanceof StringNode string) {
            return string.getValue();
        } else if (node instanceof ObjectNode objectNode) {
            if (objectNode.isNull())
                return null;

            Object existing = visited.get(objectNode);
            if (existing != null)
                return existing;

            Object object = initializeMemory(objectNode);
            visited.put(objectNode, object);

            for (var entry : objectNode.getFields().entrySet()) {
                String name = entry.getKey();
                LazyNode fieldNode = entry.getValue();
                Object value = transform(fieldNode);
                setFieldValue(object, name, value);
            }

            return object;
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node.getClass());
        }
    }

    private Object initializeMemory(ObjectNode lazyNode) {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            return ((Unsafe) unsafe.get(null)).allocateInstance(Class.forName(lazyNode.getType()));
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
