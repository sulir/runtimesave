package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.nodes.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbReader {
    private final Database db;

    public DbReader(Database db) {
        this.db = db;
    }

    public FrameNode readFrame(String frameId) {
        FrameNode frameNode = new FrameNode();

        try (Session session = db.createSession()) {
            String query = "MATCH (f:Frame)"
                    + " WHERE elementId(f) = $frameId"
                    + " MATCH (f)-[h:HAS_VARIABLE]->(v)"
                    + " RETURN elementId(v) AS variableId, h.name AS name";
            Result result = session.run(query, Map.of("frameId", frameId));

            while (result.hasNext()) {
                Record record = result.next();
                String variableId = record.get("variableId").asString();

                GraphNode variableNode = readVariable(variableId);
                frameNode.setVariable(record.get("name").asString(), variableNode);
            }

            return frameNode;
        }
    }

    public GraphNode readVariable(String variableId) {
        try (Session session = db.createSession()) {
            String query = "MATCH (v)"
                    + " WHERE elementId(v) = $variableId"
                    + " CALL apoc.path.subgraphAll(v, {relationshipFilter: '>'}) YIELD nodes, relationships"
                    + " RETURN nodes, relationships";
            Record record = session.run(query, Map.of("variableId", variableId)).single();
            List<Node> nodes = record.get("nodes").asList(Value::asNode);
            List<Relationship> edges = record.get("relationships").asList(Value::asRelationship);

            Map<String, GraphNode> idToNode = new HashMap<>();
            for (Node node : nodes)
                idToNode.put(node.elementId(), createNode(node));

            for (Relationship edge : edges) {
                GraphNode from = idToNode.get(edge.startNodeElementId());
                GraphNode to = idToNode.get(edge.endNodeElementId());
                createEdge(from, to, edge);
            }

            return idToNode.get(variableId);
        }
    }

    private GraphNode createNode(Node node) {
        String label = node.labels().iterator().next();
        return switch (label) {
            case "Primitive" -> new PrimitiveNode(node.get("value"));
            case "Null" -> new NullNode();
            case "String" -> new StringNode(node.get("value").asString());
            case "Object" -> new ObjectNode();
            case "Array" -> new ArrayNode();
            case "Type" -> TypeNode.getInstance(node.get("name").asString());
            default -> throw new IllegalArgumentException("Unknown node label: " + label);
        };
    }

    private void createEdge(GraphNode from, GraphNode to, Relationship edge) {
        if (from instanceof PrimitiveNode primitive && to instanceof TypeNode type) {
            primitive.setValue(toBoxed((Value) primitive.getValue(), type.getName()));
            primitive.setType(type.getName());
        } else if (from instanceof ReferenceNode reference && to instanceof TypeNode type)
            reference.setType(type.getName());
        else if (from instanceof ObjectNode object)
            object.setField(edge.get("name").asString(), to);
        else if (from instanceof ArrayNode array)
            array.setElement(edge.get("index").asInt(), to);
    }

    private Object toBoxed(Value value, String type) {
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
