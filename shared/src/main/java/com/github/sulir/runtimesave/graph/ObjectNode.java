package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.DBReader;
import org.neo4j.driver.types.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ObjectNode extends GraphNode {
    private final String id;
    private Map<String, GraphNode> fields;

    public ObjectNode(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public boolean isNull() {
        return type.equals("null");
    }

    public Map<String, GraphNode> getFields() {
        if (fields == null)
            loadFields();

        return fields;
    }

    public void setFields(Map<String, GraphNode> fields) {
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

    private void loadFields() {
        fields = new HashMap<>();

        DBReader.getInstance().readObjectFields(id).forEach(record -> {
            String fieldName = record.get("f").get("name").asString();
            Node dbNode = record.get("n").asNode();
            fields.put(fieldName, GraphNode.fromDB(dbNode));
        });
    }
}
