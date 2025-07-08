package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;

import java.util.SortedMap;
import java.util.TreeMap;

public class ArrayNode extends ValueNode {
    private static final Mapping mapping = mapping(ArrayNode.class)
            .property("type", String.class, ArrayNode::getType)
            .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
            .constructor(ArrayNode::new);

    private final String type;
    private final SortedMap<Integer, ValueNode> elements = new TreeMap<>();

    public ArrayNode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public ValueNode getElement(int index) {
        return elements.get(index);
    }

    public void setElement(int index, ValueNode element) {
        checkModification();
        elements.put(index, element);
    }

    public void addElement(ValueNode element) {
        setElement(elements.size(), element);
    }

    public int length() {
        return elements.isEmpty() ? 0 : elements.lastKey() + 1;
    }

    public Mapping getMapping() {
        return mapping;
    }
}
