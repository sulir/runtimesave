package com.github.sulir.runtimesave.graph;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class JavaObjectGraph {
    private final GraphNode root;
    private final Map<ReferenceNode, Object> visited = new HashMap<>();

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
        } else if (node instanceof ReferenceNode referenceNode) {
            Object existing = visited.get(referenceNode);
            if (existing != null)
                return existing;

            Object object = null;
            if (referenceNode instanceof ArrayNode arrayNode) {
                object = allocateArray(arrayNode);
                visited.put(referenceNode, object);
                assignElements(object, arrayNode);
            } else if (referenceNode instanceof ObjectNode objectNode) {
                object = allocateObject(objectNode);
                visited.put(referenceNode, object);
                assignFields(object, objectNode);
            }

            return object;
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node.getClass());
        }
    }

    private Object allocateArray(ArrayNode arrayNode) {
        try {
            String component = arrayNode.getType().substring(0, arrayNode.getType().indexOf("["));
            int dimensions = (int) arrayNode.getType().chars().filter(c -> c == '[').count();

            Class<?> componentType = Class.forName(component);
            for (int i = 0; i < dimensions - 1; i++)
                componentType = componentType.arrayType();

            int length = arrayNode.getElements().length;
            return Array.newInstance(componentType, length);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void assignElements(Object object, ArrayNode arrayNode) {
        for (int i = 0; i < arrayNode.getElements().length; i++) {
            Object element = transform(arrayNode.getElements()[i]);
            Array.set(object, i, element);
        }
    }

    private Object allocateObject(ObjectNode node) {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            return ((Unsafe) unsafe.get(null)).allocateInstance(Class.forName(node.getType()));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void assignFields(Object object, ObjectNode objectNode) {
        for (var entry : objectNode.getFields().entrySet()) {
            String name = entry.getKey();
            GraphNode fieldNode = entry.getValue();
            Object value = transform(fieldNode);
            assignField(object, name, value);
        }
    }

    private void assignField(Object object, String name, Object value) {
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
