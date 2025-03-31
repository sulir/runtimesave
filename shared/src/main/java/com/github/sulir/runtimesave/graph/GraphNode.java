package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.DBReader;
import com.github.sulir.runtimesave.db.SourceLocation;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.NoSuchRecordException;

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

    public static GraphNode findVariable(SourceLocation location, String variableName)
            throws NoMatchException {
        try {
            Record record = DBReader.getInstance().readVariable(location, variableName);
            return fromDB(record);
        } catch (NoSuchRecordException e) {
            throw new NoMatchException(String.format("Cannot find variable %s in %s.%s:%d",
                    variableName, location.getClassName(), location.getMethod(), location.getLine()));
        }
    }
}
