package com.github.sulir.runtimesave.nodes;

import java.util.*;

public class ObjectNode extends ReferenceNode {
    private final SortedMap<String, GraphNode> fields = new TreeMap<>();

    public GraphNode getField(String name) {
        return fields.get(name);
    }

    public SortedMap<String, GraphNode> getFields() {
        return fields;
    }

    public void setField(String name, GraphNode value) {
        fields.put(name, value);
    }
}
