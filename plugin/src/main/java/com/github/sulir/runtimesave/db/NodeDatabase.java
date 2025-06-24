package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.ObjectMapper;
import com.github.sulir.runtimesave.hash.NodeHash;
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

    public <T extends GraphNode> T read(NodeHash hash, Class<T> type) {
        try (Session session = db.createSession()) {
            String query = "MATCH (root {hash: $hash})"
                    + " LIMIT 1"
                    + " CALL apoc.path.subgraphAll(root, {relationshipFilter: '>'}) YIELD nodes, relationships"
                    + " RETURN elementId(root) as rootId, nodes, relationships";
            Record record = session.run(query, Map.of("hash", hash.toString())).single();
            String rootId = record.get("rootId").asString();
            List<Node> nodes = record.get("nodes").asList(Value::asNode);
            List<Relationship> edges = record.get("relationships").asList(Value::asRelationship);

            Map<String, GraphNode> idToNode = new HashMap<>();
            for (Node dbNode : nodes) {
                String label = dbNode.labels().iterator().next();
                Map<String, Value> properties = dbNode.asMap(Values.ofValue());
                GraphNode graphNode = ObjectMapper.forLabel(label).createNodeObject(properties);
                graphNode.setHash(NodeHash.fromString(properties.get("hash").asString()));
                idToNode.put(dbNode.elementId(), graphNode);
            }

            for (Relationship edge : edges) {
                GraphNode from = idToNode.get(edge.startNodeElementId());
                GraphNode to = idToNode.get(edge.endNodeElementId());
                Map<String, Value> properties = edge.asMap(Values.ofValue());
                ObjectMapper.forClass(from.getClass()).connectNodeObjects(from, to, properties);
            }

            return type.cast(idToNode.get(rootId));
        }
    }

    public void write(GraphNode rootNode) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        rootNode.traverse(node -> {
            ObjectMapper mapper = ObjectMapper.forClass(node.getClass());
            nodes.add(Map.of("label", mapper.getLabel(), "hash", node.hash().toString(),
                    "props", mapper.getProperties(node)));
            edges.addAll(mapper.getRelationships(node));
        });

        String query = "UNWIND $nodes AS node"
                + " CALL apoc.merge.node([node.label], {hash: node.hash}, node.props) YIELD node AS merged"
                + " RETURN 0";

        try (Session session = db.createSession()) {
            session.run(query, Map.of("nodes", nodes));
        }

        query = "UNWIND $edges AS edge"
                + " MATCH (from {hash: edge.from}), (to {hash: edge.to})"
                + " CALL apoc.merge.relationship(from, edge.type, edge.props, {}, to) YIELD rel"
                + " RETURN 0";

        try (Session session = db.createSession()) {
            session.run(query, Map.of("edges", edges));
        }
    }
}
