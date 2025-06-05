package com.github.sulir.runtimesave.graph;

import org.neo4j.driver.Record;

public abstract class ReferenceNode extends GraphNode {
    private final String id;
    private final String type;

    public static ReferenceNode fromDB(Record record) {
        String id = record.get("v").get("id").asString();
        String type = record.get("t").get("name").asString();

        if (type.contains("["))
            return new ArrayNode(id, type);
        else
            return new ObjectNode(id, type);
    }

    public ReferenceNode(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReferenceNode other))
            return false;

        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
