package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;

import java.util.*;

public class ObjectNode extends ValueNode {
    public static final Mapping mapping = mapping(ObjectNode.class)
            .property("type", String.class, ObjectNode::getType)
            .edges("HAS_FIELD", "name", String.class, ValueNode.class, node -> node.fields)
            .constructor(ObjectNode::new);

    private final String type;
    private final SortedMap<String, ValueNode> fields = new TreeMap<>();

    public ObjectNode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public ValueNode getField(String name) {
        return fields.get(name);
    }

    public Collection<String> getFieldNames() {
        return Collections.unmodifiableCollection(fields.keySet());
    }

    public void setField(String name, ValueNode field) {
        checkModification();
        fields.put(name, field);
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }
}
