package com.github.sulir.runtimesave.graph;

import java.util.Map;
import java.util.Objects;

public class ObjectNode extends LazyNode {
    private final String id;
    private Map<String, LazyNode> fields;

    public ObjectNode(String id) {
        this.id = id;
    }

    public ObjectNode(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public boolean isNull() {
        return type.equals("null");
    }

    public Map<String, LazyNode> getFields() {
        return fields;
    }

    public void setFields(Map<String, LazyNode> fields) {
        this.fields = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjectNode other))
            return false;

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
