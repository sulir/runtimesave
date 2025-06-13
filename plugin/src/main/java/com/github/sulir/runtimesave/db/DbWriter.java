package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.nodes.*;
import org.neo4j.driver.Session;

import java.util.*;

public class DbWriter {
    private final Database db;

    public DbWriter(Database db) {
        this.db = db;
    }

    public String writeNode(GraphNode variableNode) {
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
        ObjectMapper mapper = ObjectMapper.getInstance(node.getClass());
        String label = mapper.getLabel();
        Map<String, Object> properties = mapper.createProperties(node);

        nodes.add(Map.of("label", label, "properties", properties));
        nodeToId.put(node, (String) properties.get("id"));

        for (GraphNode target : node.iterate()) {
            if (!nodeToId.containsKey(target))
                traverse(target, nodes, nodeToId, edges);
        }

        edges.addAll(mapper.createOutEdges(node, nodeToId));
    }
}
