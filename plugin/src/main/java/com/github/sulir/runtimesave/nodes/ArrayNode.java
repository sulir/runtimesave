package com.github.sulir.runtimesave.nodes;

import java.util.ArrayList;
import java.util.List;

public class ArrayNode extends GraphNode {
    private final String type;
    private final ArrayList<GraphNode> elements = new ArrayList<>();

    public ArrayNode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public List<GraphNode> getElements() {
        return elements;
    }

    public GraphNode getElement(int index) {
        return elements.get(index);
    }

    public void setElement(int index, GraphNode element) {
        setSize(index + 1);
        elements.set(index, element);
    }

    public int getSize() {
        return elements.size();
    }

    public void setSize(int size) {
        elements.ensureCapacity(size);
        while (elements.size() < size)
            elements.add(null);
    }

    @Override
    public Iterable<GraphNode> iterate() {
        return elements;
    }
}
