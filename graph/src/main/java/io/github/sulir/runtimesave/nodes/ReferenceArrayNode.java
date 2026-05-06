package io.github.sulir.runtimesave.nodes;

import io.github.sulir.runtimesave.graph.Mapping;
import io.github.sulir.runtimesave.graph.ValueNode;
import io.github.sulir.runtimesave.misc.ArrayMap;

import java.util.SortedMap;
import java.util.function.BiConsumer;

public class ReferenceArrayNode extends ValueNode {
    public static final Mapping mapping = mapping(ReferenceArrayNode.class)
            .property("type", String.class, ReferenceArrayNode::getType)
            .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
            .constructor((String t) -> new ReferenceArrayNode(t));

    private String type;
    private int fullLength = -1;
    protected final SortedMap<Integer, ValueNode> elements = new ArrayMap<>();

    public ReferenceArrayNode(String type) {
        checkType(type);
        this.type = type;
    }

    public ReferenceArrayNode(int fullLength) {
        this.fullLength = fullLength;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        checkType(type);
        checkModification();
        this.type = type;
    }

    public int getFullLength() {
        return fullLength;
    }

    public String getComponentType() {
        return getComponentType(type);
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

    private static void checkType(String type) {
        if (!type.endsWith("[]") || PrimitiveArrayNode.isPrimitive(getComponentType(type)))
            throw new IllegalArgumentException("Invalid reference array type: " + type);
    }

    private static String getComponentType(String type) {
        return type.substring(0, type.length() - 2);
    }
}
