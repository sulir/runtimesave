package com.github.sulir.runtimesave.graph;

public class GraphSaver {
    private final Object value;
    private final boolean primitive;

    public GraphSaver(Object value) {
        this(value, false);
    }

    public GraphSaver(Object value, boolean primitive) {
        this.value = value;
        this.primitive = primitive;
    }

    public GraphNode save() {
        if (primitive)
            return new PrimitiveNode(value);
        else
            return new ObjectNode("", value.getClass().getName());
    }
}
