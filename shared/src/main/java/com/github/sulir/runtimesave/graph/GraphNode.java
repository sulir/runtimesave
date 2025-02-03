package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.DBReader;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

public abstract class GraphNode {
    protected String type;

    public String getType() {
        return type;
    }

    public static GraphNode fromDB(Node dbNode) {
        String label = dbNode.labels().iterator().next();

        return switch (label) {
            case "Value" -> new PrimitiveNode(convertNodeValue(dbNode), dbNode.get("type").asString());
            case "String" -> new StringNode(dbNode.get("value").asString());
            case "Object" -> new ObjectNode(dbNode.get("id").asString(), dbNode.get("type").asString());
            default -> throw new IllegalArgumentException("Unknown node label: " + label);
        };
    }

    public static GraphNode findVariable(String className, String method, String variableName) {
        Node dbNode = DBReader.getInstance().readVariable(className, method, variableName);
        return fromDB(dbNode);
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
