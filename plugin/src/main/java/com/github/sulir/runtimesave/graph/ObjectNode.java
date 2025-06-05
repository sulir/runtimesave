package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.DBReader;

import java.util.HashMap;
import java.util.Map;

public class ObjectNode extends ReferenceNode {
    private Map<String, GraphNode> fields;

    public ObjectNode(String id, String type) {
        super(id, type);
    }

    public Map<String, GraphNode> getFields() {
        if (fields == null)
            loadFields();

        return fields;
    }

    public GraphNode getField(String name) {
        return getFields().get(name);
    }

    private void loadFields() {
        fields = new HashMap<>();

        DBReader.getInstance().readObjectFields(getId()).forEach(record -> {
            String fieldName = record.get("f").get("name").asString();
            fields.put(fieldName, GraphNode.fromDB(record));
        });
    }
}
