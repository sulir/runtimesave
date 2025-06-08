package com.github.sulir.runtimesave.nodes;

import java.util.ArrayList;

public class ArrayNode extends ReferenceNode {
    private final ArrayList<GraphNode> elements = new ArrayList<>();

    public GraphNode getElement(int index) {
        return elements.get(index);
    }

    public void setElement(int index, GraphNode value) {
        elements.ensureCapacity(index + 1);
        while (elements.size() <= index)
            elements.add(null);

        elements.set(index, value);
    }

    public int getSize() {
        return elements.size();
    }
}
