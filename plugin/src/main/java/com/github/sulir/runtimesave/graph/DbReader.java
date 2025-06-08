package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.Database;
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

    public FrameNode readFrame(String elementId) {
        FrameNode frameNode = new FrameNode();

        try (Session session = db.createSession()) {
            String query = "MATCH (l:Line)"
                    + " WHERE elementId(l) = $elementId"
                    + " MATCH (l)-[h:HAS_VARIABLE]->(v)"
                    + " RETURN elementId(v) AS variableId, h.name AS name";
            Result result = session.run(query, Map.of("elementId", elementId));

            while (result.hasNext()) {
                Record record = result.next();
                String variableId = record.get("variableId").asString();

                GraphNode variableNode = readVariable(variableId);
                frameNode.addVariable(record.get("name").asString(), variableNode);
            }

            return frameNode;
        }
    }

    public GraphNode readVariable(String elementId) {
        try (Session session = db.createSession()) {
            String query = "MATCH (v)"
                    + " WHERE elementId(v) = $elementId"
                    + " CALL apoc.path.subgraphAll(v, {relationshipFilter: '>'}) YIELD nodes, relationships"
                    + " RETURN nodes, relationships";
            Record record = session.run(query, Map.of("elementId", elementId)).single();
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

            return idToNode.get(elementId);
        }
    }

    private GraphNode createNode(Node node) {
        String label = node.labels().iterator().next();
        return switch (label) {
            case "Primitive" -> new PrimitiveNode(node.get("value"));
            case "Null" -> new NullNode();
            case "Object" -> new ObjectNode();
            case "Array" -> new ArrayNode();
            case "String" -> new StringNode(node.get("value").asString());
            case "Type" -> new TypeNode(node.get("name").asString());
            default -> throw new IllegalArgumentException("Unknown node label: " + label);
        };
    }

    private void createEdge(GraphNode from, GraphNode to, Relationship edge) {
        if (from instanceof PrimitiveNode primitive && to instanceof TypeNode type)
            primitive.setValue(dbToBoxedType((Value) primitive.getValue(), type.getName()));
        else if (from instanceof ReferenceNode reference && to instanceof TypeNode type)
            reference.setType(type.getName());
        else if (from instanceof ObjectNode object)
            object.addField(edge.get("name").asString(), to);
        else if (from instanceof ArrayNode array)
            array.setElement(edge.get("index").asInt(), to);
    }

    private Object dbToBoxedType(Value value, String type) {
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
