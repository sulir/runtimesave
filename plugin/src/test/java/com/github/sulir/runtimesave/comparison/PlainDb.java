package com.github.sulir.runtimesave.comparison;

import com.github.sulir.runtimesave.db.Database;
import com.github.sulir.runtimesave.db.DbConnection;
import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.NodeProperty;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;

import java.util.*;

public class PlainDb extends Database {
    public static final String INDEX_LABEL = "Node";
    public static final String INDEX_PROPERTY = "id";

    public PlainDb(DbConnection db) {
        super(db);
    }

    public String write(GraphNode root) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<GraphNode, String> nodeToId = new java.util.HashMap<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        traverse(root, nodes, nodeToId, edges);

        db.writeTransaction(transaction -> {
            writeNodes(nodes, transaction);
            writeEdges(edges, transaction);
        });

        return nodeToId.get(root);
    }

    public void addNote(String nodeId, String text) {
        String query = "MATCH (v:" + INDEX_LABEL + " {" + INDEX_PROPERTY + ": $id})"
                + " CREATE (n:Note:Meta {text: $text})-[:DESCRIBES]->(v)";
        try (Session session = db.createSession()) {
            session.run(query, Map.of("id", nodeId, "text", text));
        }
    }

    private void traverse(GraphNode node, List<Map<String, Object>> nodes, Map<GraphNode, String> nodeToId,
                          List<Map<String, Object>> edges) {
        if (nodeToId.containsKey(node))
            return;

        String id = UUID.randomUUID().toString();
        nodeToId.put(node, id);
        Map<String, Object> properties = getPropertyMap(node, id);
        nodes.add(Map.of("label", node.label(), "props", properties));

        node.targets().forEach(target -> traverse(target, nodes, nodeToId, edges));
        edges.addAll(getRelationships(node, nodeToId));
    }

    private Map<String, Object> getPropertyMap(GraphNode node, String id) {
        Map<String, Object> properties = new HashMap<>();
        for (NodeProperty property : node.properties())
            properties.put(property.key(), property.value());

        properties.put(INDEX_PROPERTY, id);
        return properties;
    }

    private List<Map<String, Object>> getRelationships(GraphNode node, Map<GraphNode, String> nodeToId) {
        List<Map<String, Object>> edges = new ArrayList<>();
        String from = nodeToId.get(node);

        node.forEachEdge((label, target) -> edges.add(Map.of(
                "from", from,
                "to", nodeToId.get(target),
                "type", node.getMapping().relation().type(),
                "props", Map.of(node.getMapping().relation().propertyName(), label)
        )));
        return edges;
    }

    private void writeNodes(List<Map<String, Object>> nodes, TransactionContext transaction) {
        String query = "FOREACH (node in $nodes |"
                + " CREATE (n:$(node.label):" + INDEX_LABEL + ")"
                + " SET n = node.props"
                + ")";
        transaction.run(query, Map.of("nodes", nodes));
    }

    private void writeEdges(List<Map<String, Object>> edges, TransactionContext transaction) {
        String query = "UNWIND $edges AS edge"
                + " MATCH (from:" + INDEX_LABEL + " {" + INDEX_PROPERTY + ": edge.from})"
                + " MATCH (to:" + INDEX_LABEL + " {" + INDEX_PROPERTY + ": edge.to})"
                + " CALL apoc.create.relationship(from, edge.type, edge.props, to) YIELD rel"
                + " RETURN 0";

        transaction.run(query, Map.of("edges", edges));
    }
}
