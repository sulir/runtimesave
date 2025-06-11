package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.nodes.*;
import org.neo4j.driver.Session;

import java.util.*;
import java.util.stream.Stream;

public class DbWriter {
    private final Database db;

    public DbWriter(Database db) {
        this.db = db;
    }

    public String write(GraphNode variableNode) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<GraphNode, String> nodeToId = new java.util.HashMap<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        traverse(variableNode, nodes, nodeToId, edges);

        String query = "UNWIND $nodes AS node"
                + " CALL apoc.create.node([node.label], node.properties) YIELD node AS created "
                + " WITH elementId(collect(created)[0]) AS variableId"
                + " UNWIND $edges AS edge"
                + " MATCH (f {id: edge.from}), (t {id: edge.to})"
                + " CALL apoc.create.relationship(f, edge.type, edge.props, t) YIELD rel"
                + " WITH variableId"
                + " MATCH (n)"
                + " REMOVE n.id"
                + " RETURN variableId"
                + " LIMIT 1";

        try (Session session = db.createSession()) {
            return session.run(query, Map.of("nodes", nodes, "edges", edges)).single().get("variableId").asString();
        }
    }

    private void traverse(GraphNode node, List<Map<String, Object>> nodes, Map<GraphNode, String> nodeToId,
                          List<Map<String, Object>> edges) {
        String label = createLabel(node);
        Map<String, Object> properties = createProperties(node);

        nodes.add(Map.of("label", label, "properties", properties));
        nodeToId.put(node, (String) properties.get("id"));

        for (GraphNode target : iterate(node)) {
            if (!nodeToId.containsKey(target))
                traverse(target, nodes, nodeToId, edges);
        }

        edges.addAll(createOutEdges(node, nodeToId));
    }

    private String createLabel(GraphNode node) {
        String className = node.getClass().getSimpleName();
        return className.substring(0, className.lastIndexOf("Node"));
    }

    private Map<String, Object> createProperties(GraphNode node) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", UUID.randomUUID().toString());

        if (node instanceof PrimitiveNode primitive)
            properties.put("value", primitive.getValue());
        else if (node instanceof StringNode string)
            properties.put("value", string.getValue());
        else if (node instanceof TypeNode type)
            properties.put("name", type.getName());

        return properties;
    }

    private Iterable<GraphNode> iterate(GraphNode node) {
        if (node instanceof FrameNode frame)
            return frame.getVariables().values();
        else if (node instanceof PrimitiveNode primitive)
            return List.of(TypeNode.getInstance(primitive.getType()));
        else if (node instanceof ObjectNode object)
            return Stream.concat(Stream.of(TypeNode.getInstance(object.getType())),
                    object.getFields().values().stream())::iterator;
        else if (node instanceof ArrayNode array)
            return Stream.concat(Stream.of(TypeNode.getInstance(array.getType())),
                    array.getElements().stream())::iterator;
        else
            return List.of();
    }

    private List<Map<String, Object>> createOutEdges(GraphNode node, Map<GraphNode, String> nodeToId) {
        List<Map<String, Object>> edges = new ArrayList<>();
        String from = nodeToId.get(node);

        if (node instanceof FrameNode frame) {
            frame.getVariables().forEach((key, value) ->
                    edges.add(Map.of("from", from, "type", "HAS_VARIABLE",
                            "props", Map.of("name", key), "to", nodeToId.get(value)))
            );
        } else if (node instanceof PrimitiveNode primitive) {
            TypeNode type = TypeNode.getInstance(primitive.getType());
            edges.add(Map.of("from", from, "type", "HAS_TYPE", "props", Map.of(), "to", nodeToId.get(type)));
        } else if (node instanceof ReferenceNode reference) {
            TypeNode type = TypeNode.getInstance(reference.getType());
            edges.add(Map.of("from", from, "type", "HAS_TYPE", "props", Map.of(), "to", nodeToId.get(type)));

            if (node instanceof ObjectNode object) {
                object.getFields().forEach((key, value) ->
                        edges.add(Map.of("from", from, "type", "HAS_FIELD",
                                "props", Map.of("name", key), "to", nodeToId.get(value)))
                );
            } else if (node instanceof ArrayNode array) {
                int index = 0;
                for (GraphNode element : array.getElements()) {
                    edges.add(Map.of("from", from, "type", "HAS_ELEMENT",
                            "props", Map.of("index", index++), "to", nodeToId.get(element)));
                }
            }
        }

        return edges;
    }
}
