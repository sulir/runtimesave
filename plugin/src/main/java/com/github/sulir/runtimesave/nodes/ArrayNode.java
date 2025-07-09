package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class ArrayNode extends ValueNode {
    private static final Mapping mapping = mapping(ArrayNode.class)
            .property("type", String.class, ArrayNode::getType)
            .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
            .constructor(ArrayNode::new);

    private final String type;
    protected final SortedMap<Integer, ValueNode> elements = new TreeMap<>();

    public ArrayNode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getComponentType() {
        String component = type.substring(1);
        if (component.startsWith("["))
            return component;

        if (component.startsWith("L") && component.endsWith(";"))
            return component.substring(1, component.length() - 1);

        return switch (component) {
            case "C" -> "char";
            case "B" -> "byte";
            case "S" -> "short";
            case "I" -> "int";
            case "J" -> "long";
            case "F" -> "float";
            case "D" -> "double";
            case "Z" -> "boolean";
            default -> throw new IllegalArgumentException("Unknown array component: " + component);
        };
    }

    public ValueNode getElement(int index) {
        return elements.get(index);
    }

    public void forEachElement(BiConsumer<Integer, ValueNode> action) {
        elements.forEach(action);
    }

    public void setElement(int index, ValueNode element) {
        checkModification();
        elements.put(index, element);
    }

    public void addElement(ValueNode element) {
        setElement(getLength(), element);
    }

    public int getLength() {
        return elements.isEmpty() ? 0 : elements.lastKey() + 1;
    }

    public Mapping getMapping() {
        return mapping;
    }
}
