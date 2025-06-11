package com.github.sulir.runtimesave.nodes;

import java.util.ArrayList;
import java.util.List;

public class ArrayNode extends ReferenceNode {
    private final ArrayList<GraphNode> elements = new ArrayList<>();

    public GraphNode getElement(int index) {
        return elements.get(index);
    }

    public List<GraphNode> getElements() {
        return elements;
    }

    public void setElement(int index, GraphNode value) {
        setSize(index + 1);
        elements.set(index, value);
    }

    public int getSize() {
        return elements.size();
    }

    public void setSize(int size) {
        elements.ensureCapacity(size);
        while (elements.size() < size)
            elements.add(null);
    }
}
