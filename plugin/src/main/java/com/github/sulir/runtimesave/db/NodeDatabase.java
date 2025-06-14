package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.ObjectMapper;
import com.github.sulir.runtimesave.nodes.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;

public class NodeDatabase {
    private final DbConnection db;

    public NodeDatabase(DbConnection db) {
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
            for (Node node : nodes) {
                String label = node.labels().iterator().next();
                Map<String, Value> properties = node.asMap(Values.ofValue());
                idToNode.put(node.elementId(), ObjectMapper.forLabel(label).createNodeObject(properties));
            }

            for (Relationship edge : edges) {
                GraphNode from = idToNode.get(edge.startNodeElementId());
                GraphNode to = idToNode.get(edge.endNodeElementId());
                Map<String, Value> properties = edge.asMap(Values.ofValue());
                ObjectMapper.forClass(from.getClass()).connectNodeObjects(from, to, properties);
            }

            return type.cast(idToNode.get(elementId));
        }
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
        ObjectMapper mapper = ObjectMapper.forClass(node.getClass());

        Map<String, Object> properties = mapper.getProperties(node);
        String id = UUID.randomUUID().toString();
        properties.put("id", id);

        nodes.add(Map.of("label", mapper.getLabel(), "properties", properties));
        nodeToId.put(node, id);

        for (GraphNode target : node.iterate()) {
            if (!nodeToId.containsKey(target))
                traverse(target, nodes, nodeToId, edges);
        }

        edges.addAll(mapper.getRelationships(node, nodeToId));
    }
}
