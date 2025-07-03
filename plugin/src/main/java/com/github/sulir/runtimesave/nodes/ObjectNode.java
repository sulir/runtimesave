package com.github.sulir.runtimesave.nodes;

import java.util.*;

public class ObjectNode extends ValueNode {
    private final String type;
    private SortedMap<String, ValueNode> fields = new TreeMap<>();

    public ObjectNode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public SortedMap<String, ValueNode> outEdges() {
        return fields;
    }

    public ValueNode getField(String name) {
        return fields.get(name);
    }

    public void setField(String name, ValueNode field) {
        fields.put(name, field);
    }

    @Override
    public void freeze() {
        fields = Collections.unmodifiableSortedMap(fields);
    }
}
