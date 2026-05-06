package io.github.sulir.runtimesave.nodes;

import io.github.sulir.runtimesave.graph.Mapping;
import io.github.sulir.runtimesave.graph.ValueNode;
import io.github.sulir.runtimesave.misc.ArrayMap;

import java.util.SortedMap;

public class ReferenceArrayNode extends ValueNode {
    public static final Mapping mapping = mapping(ReferenceArrayNode.class)
            .property("type", String.class, ReferenceArrayNode::getType)
            .property("length", Integer.class, ReferenceArrayNode::getLength)
            .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
            .argsConstructor(props -> new ReferenceArrayNode((String) props[0], (int) props[1]));

    private String type;
    private final int length;
    protected final SortedMap<Integer, ValueNode> elements;

    public ReferenceArrayNode(String type, int length) {
        this(length);
        checkType(type);
        this.type = type;
    }

    public ReferenceArrayNode(int length) {
        this.length = length;
        elements = new ArrayMap<>(length);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        checkType(type);
        checkModification();
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public ValueNode getElement(int index) {
        return elements.get(index);
    }

    public void setElement(int index, ValueNode value) {
        checkModification();
        elements.put(index, value);
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
