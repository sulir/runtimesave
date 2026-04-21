package io.github.sulir.runtimesave.nodes;

import io.github.sulir.runtimesave.graph.Mapping;
import io.github.sulir.runtimesave.graph.ValueNode;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class ArrayNode extends ValueNode {
    public static final Mapping mapping = mapping(ArrayNode.class)
            .property("type", String.class, ArrayNode::getType)
            .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
            .constructor((String t) -> new ArrayNode(t));

    private String type;
    private int fullLength = -1;
    protected final SortedMap<Integer, ValueNode> elements = new TreeMap<>();

    public ArrayNode(String type) {
        if (!type.endsWith("[]"))
            throw new IllegalArgumentException("Invalid array type: " + type);
        this.type = type;
    }

    public ArrayNode(int fullLength) {
        this.fullLength = fullLength;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        checkModification();
        this.type = type;
    }

    public int getFullLength() {
        return fullLength;
    }

    public String getComponentType() {
        return type.substring(0, type.length() - 2);
    }

    public ValueNode getElement(int index) {
        return elements.get(index);
    }

    public void forEachElement(BiConsumer<Integer, ValueNode> action) {
        elements.forEach(action);
    }

    public void setElement(int index, ValueNode value) {
        checkModification();
        elements.put(index, value);
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
