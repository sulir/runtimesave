package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.nodes.*;
import org.neo4j.driver.Record;
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

    public <T extends GraphNode> T read(String elementId, Class<T> type) {
        try (Session session = db.createSession()) {
            String query = "MATCH (v)"
                    + " WHERE elementId(v) = $variableId"
                    + " CALL apoc.path.subgraphAll(v, {relationshipFilter: '>'}) YIELD nodes, relationships"
                    + " RETURN nodes, relationships";
            Record record = session.run(query, Map.of("variableId", elementId)).single();
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

            return type.cast(idToNode.get(elementId));
        }
    }

    private GraphNode createNode(Node node) {
        String label = node.labels().iterator().next();
        String type = node.get("type").asString();

        return switch (label) {
            case "Frame" -> new FrameNode();
            case "Primitive" -> new PrimitiveNode(toBoxed(node.get("value"), type), type);
            case "Null" -> new NullNode();
            case "String" -> new StringNode(node.get("value").asString());
            case "Array" -> new ArrayNode(type);
            case "Object" -> new ObjectNode(type);
            default -> throw new IllegalArgumentException("Unknown node label: " + label);
        };
    }

    private void createEdge(GraphNode from, GraphNode to, Relationship edge) {
        if (from instanceof FrameNode frame)
            frame.setVariable(edge.get("name").asString(), to);
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
