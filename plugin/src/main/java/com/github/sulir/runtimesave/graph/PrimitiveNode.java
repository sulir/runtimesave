package com.github.sulir.runtimesave.graph;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

public class PrimitiveNode extends GraphNode {
    private final Object value;

    public static PrimitiveNode fromDB(Record record) {
        String type = record.get("t").get("name").asString();
        Value value = record.get("v").get("value");

        Object result = switch (type) {
            case "char" -> value.asString().charAt(0);
            case "byte" -> (byte) value.asInt();
            case "short" -> (short) value.asInt();
            case "int" -> value.asInt();
            case "long" -> value.asLong();
            case "float" -> value.asFloat();
            case "double" -> value.asDouble();
            case "boolean" -> value.asBoolean();
            default -> throw new IllegalArgumentException("Unknown primitive type: " + type);
        };

        return new PrimitiveNode(result);
    }

    public PrimitiveNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
