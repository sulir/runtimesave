package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.ObjectMapper;
import com.github.sulir.runtimesave.hash.AcyclicGraph;
import com.github.sulir.runtimesave.hash.NodeHash;
import com.github.sulir.runtimesave.hash.StrongComponent;
import com.github.sulir.runtimesave.nodes.GraphNode;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;
import java.util.stream.Stream;

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

    public void write(AcyclicGraph dag) {
        Set<StrongComponent> visited = new HashSet<>();
        List<StrongComponent> created = new ArrayList<>();
        writeComponent(dag.getRootComponent(), visited, created);
        writeOutEdges(created.stream().flatMap(scc -> scc.nodes().stream()));
    }

    private void writeComponent(StrongComponent scc, Set<StrongComponent> visited, List<StrongComponent> created) {
        visited.add(scc);
        if (writeNode(scc.getFirstNode())) {
            created.add(scc);
            writeNodes(scc.getRestOfNodes());

            for (StrongComponent target : scc.targets())
                if (!visited.contains(target))
                    writeComponent(target, visited, created);
        }
    }

    private boolean writeNode(GraphNode node) {
        Map<String, Object> nodeMap = nodeToMap(node);

        String query = "CALL apoc.merge.nodeWithStats([$node.label], {idHash: $node.idHash}, $node.props)"
                + " YIELD stats, node"
                + " RETURN stats.nodesCreated > 0 AS created";

        try (Session session = db.createSession()) {
            return session.run(query, Map.of("node", nodeMap)).single().get("created").asBoolean();
        }
    }

    private void writeNodes(Set<GraphNode> nodes) {
        List<Map<String, Object>> nodesMap = nodes.stream().map(this::nodeToMap).toList();
        if (nodesMap.isEmpty())
            return;

        String query = "UNWIND $nodes AS node"
                + " CALL apoc.merge.node([node.label], {idHash: node.idHash}, node.props) YIELD node AS merged"
                + " RETURN 0";

        try (Session session = db.createSession()) {
            session.run(query, Map.of("nodes", nodesMap));
        }
    }

    private Map<String, Object> nodeToMap(GraphNode node) {
        ObjectMapper mapper = ObjectMapper.forClass(node.getClass());
        Map<String, Object> properties = mapper.getProperties(node);
        properties.put("hash", node.hash().toString());
        return Map.of("label", mapper.getLabel(), "idHash", node.idHash().toString(), "props", properties);
    }

    private void writeOutEdges(Stream<GraphNode> nodes) {
        List<Map<String, Object>> edges = new ArrayList<>();
        nodes.forEach(node -> {
            ObjectMapper mapper = ObjectMapper.forClass(node.getClass());
            edges.addAll(mapper.getRelationships(node));
        });
        if (edges.isEmpty())
            return;

        String query = "UNWIND $edges AS edge"
                + " MATCH (from {idHash: edge.from})"
                + " MATCH (to {idHash: edge.to})"
                + " CALL apoc.create.relationship(from, edge.type, edge.props, to) YIELD rel"
                + " RETURN 0";

        try (Session session = db.createSession()) {
            session.run(query, Map.of("edges", edges));
        }
    }
}
