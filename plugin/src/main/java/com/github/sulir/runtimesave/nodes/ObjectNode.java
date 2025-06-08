package com.github.sulir.runtimesave.nodes;

import java.util.HashMap;
import java.util.Map;

public class ObjectNode extends ReferenceNode {
    private final Map<String, GraphNode> fields = new HashMap<>();

    public GraphNode getField(String name) {
        return fields.get(name);
    }

    public void addField(String name, GraphNode value) {
        fields.put(name, value);
    }
}
