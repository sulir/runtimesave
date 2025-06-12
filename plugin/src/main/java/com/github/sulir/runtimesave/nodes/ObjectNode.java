package com.github.sulir.runtimesave.nodes;

import java.util.*;

public class ObjectNode extends GraphNode {
    private final String type;
    private final SortedMap<String, GraphNode> fields = new TreeMap<>();

    public ObjectNode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public SortedMap<String, GraphNode> getFields() {
        return fields;
    }

    public GraphNode getField(String name) {
        return fields.get(name);
    }

    public void setField(String name, GraphNode field) {
        fields.put(name, field);
    }
}
