package com.github.sulir.runtimesave.nodes;

import java.util.SortedMap;
import java.util.TreeMap;

public class ArrayNode extends GraphNode {
    private final String type;
    private final SortedMap<Integer, GraphNode> elements = new TreeMap<>();

    public ArrayNode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public SortedMap<Integer, GraphNode> getElements() {
        return elements;
    }

    public GraphNode getElement(int index) {
        return elements.get(index);
    }

    public void setElement(int index, GraphNode element) {
        elements.put(index, element);
    }

    public int getLength() {
        return elements.isEmpty() ? 0 : elements.lastKey() + 1;
    }

    @Override
    public SortedMap<Integer, GraphNode> outEdges() {
        return getElements();
    }
}
