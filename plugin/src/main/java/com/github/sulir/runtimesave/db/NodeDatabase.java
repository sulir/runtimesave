package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.NodeFactory;
import com.github.sulir.runtimesave.graph.NodeProperty;
import com.github.sulir.runtimesave.hash.AcyclicGraph;
import com.github.sulir.runtimesave.hash.NodeHash;
import com.github.sulir.runtimesave.hash.StrongComponent;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;
import java.util.stream.Stream;

public class NodeDatabase {
    private final DbConnection db;
    private final NodeFactory factory;
    private TransactionContext transaction;

    public NodeDatabase(DbConnection db, NodeFactory factory) {
        this.db = db;
        this.factory = factory;
    }

    public <T extends GraphNode> T read(NodeHash hash, Class<T> type) {
        try (Session session = db.createSession()) {
            String query = "MATCH (root:Hashed {hash: $hash})"
                    + " LIMIT 1"
                    + " CALL apoc.path.subgraphAll(root, {relationshipFilter: '>'}) YIELD nodes, relationships"
                    + " RETURN elementId(root) as rootId, nodes, relationships";
            Record record = session.run(query, Map.of("hash", hash.toString())).single();
            String rootId = record.get("rootId").asString();
            List<Node> nodes = record.get("nodes").asList(Value::asNode);
            List<Relationship> edges = record.get("relationships").asList(Value::asRelationship);

            Map<String, GraphNode> idToNode = createNodeObjects(nodes);
            connectNodes(edges, idToNode);
            return type.cast(idToNode.get(rootId));
        }
    }

    private Map<String, GraphNode> createNodeObjects(List<Node> nodes) {
        Map<String, GraphNode> idToNode = new HashMap<>();
        for (Node dbNode : nodes) {
            Map<String, Value> properties = dbNode.asMap(Values.ofValue());
            GraphNode graphNode = factory.createNode(dbNode.labels(), properties);
            idToNode.put(dbNode.elementId(), graphNode);
        }
        return idToNode;
    }

    private void connectNodes(List<Relationship> edges, Map<String, GraphNode> idToNode) {
        for (Relationship edge : edges) {
            GraphNode from = idToNode.get(edge.startNodeElementId());
            if (!edge.hasType(from.getMapping().relation().type()))
                continue;
            GraphNode to = idToNode.get(edge.endNodeElementId());
            Map<String, Value> properties = edge.asMap(Values.ofValue());
            Value key = properties.get(from.getMapping().relation().propertyName());
            from.setTarget(key.as(from.getMapping().relation().propertyType()), to);
        }
    }

    public void write(AcyclicGraph dag) {
        Set<StrongComponent> visited = new HashSet<>();
        List<StrongComponent> created = new ArrayList<>();

        db.writeTransaction(transaction -> {
            this.transaction = transaction;
            writeComponent(dag.getRootComponent(), visited, created);
            writeOutEdges(created.stream().flatMap(scc -> scc.nodes().stream()));
        });
        transaction = null;
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
        String query = "MERGE (n:$($label):Hashed {idHash: $idHash})" +
                " ON CREATE SET n += $props";

        return transaction.run(query, nodeToMap(node)).consume().counters().nodesCreated() > 0;
    }

    private void writeNodes(Set<GraphNode> nodes) {
        List<Map<String, Object>> nodesList = nodes.stream().map(this::nodeToMap).toList();
        if (nodesList.isEmpty())
            return;

        String query = "UNWIND $nodes AS node"
                + " MERGE (n:$(node.label):Hashed {idHash: node.idHash})"
                + " ON CREATE SET n += node.props";

        transaction.run(query, Map.of("nodes", nodesList));
    }

    private Map<String, Object> nodeToMap(GraphNode node) {
        Map<String, Object> properties = new HashMap<>();
        for (NodeProperty property : node.properties())
            properties.put(property.key(), property.value());
        properties.put("hash", node.hash().toString());

        return Map.of("label", node.label(), "idHash", node.idHash().toString(), "props", properties);
    }

    private void writeOutEdges(Stream<GraphNode> nodes) {
        List<Map<String, Object>> edges = nodes.flatMap(this::getRelationships).toList();
        if (edges.isEmpty())
            return;

        String query = "UNWIND $edges AS edge"
                + " MATCH (from:Hashed {idHash: edge.from})"
                + " MATCH (to:Hashed {idHash: edge.to})"
                + " CREATE (from)-[rel:$(edge.type)]->(to)"
                + " SET rel = edge.props";

        transaction.run(query, Map.of("edges", edges));
    }

    private Stream<Map<String, Object>> getRelationships(GraphNode from) {
        return from.edges().map(edge -> Map.of(
                "from", from.idHash().toString(),
                "to", edge.target().idHash().toString(),
                "type", from.getMapping().relation().type(),
                "props", Map.of(from.getMapping().relation().propertyName(), edge.label())));
    }
}
