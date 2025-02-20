package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.DBReader;
import org.neo4j.driver.Record;

public abstract class GraphNode {
    public static GraphNode fromDB(Record record) {
        String label = record.get("v").asNode().labels().iterator().next();

        return switch (label) {
            case "Primitive" -> PrimitiveNode.fromDB(record);
            case "Null" -> new NullNode();
            case "String" -> StringNode.fromDB(record);
            case "Array", "Object" -> ReferenceNode.fromDB(record);
            default -> throw new IllegalArgumentException("Unknown node label: " + label);
        };
    }

    public static GraphNode findVariable(String className, String method, String variableName) {
        Record record = DBReader.getInstance().readVariable(className, method, variableName);
        return fromDB(record);
    }
}
