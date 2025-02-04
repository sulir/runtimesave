package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.DBReader;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

public abstract class GraphNode {
    protected String type;

    public String getType() {
        return type;
    }

    public static GraphNode fromDB(Record record) {
        Node value = record.get("v").asNode();
        String label = value.labels().iterator().next();
        String type = record.get("t").isNull() ? null : record.get("t").get("name").asString();

        return switch (label) {
            case "Primitive" -> new PrimitiveNode(convertNodeValue(value), value.get("type").asString());
            case "String" -> new StringNode(value.get("value").asString());
            case "Object" -> new ObjectNode(value.get("id").asString(), type);
            default -> throw new IllegalArgumentException("Unknown node label: " + label);
        };
    }

    public static GraphNode findVariable(String className, String method, String variableName) {
        Record record = DBReader.getInstance().readVariable(className, method, variableName);
        return fromDB(record);
    }

    private static Object convertNodeValue(Node dbNode) {
        String type = dbNode.get("type").asString();
        Value value = dbNode.get("value");

        return switch (type) {
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
    }
}
