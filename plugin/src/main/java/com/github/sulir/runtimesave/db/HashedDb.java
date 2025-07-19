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

public class HashedDb extends Database {
    private final NodeFactory factory;

    public HashedDb(DbConnection db, NodeFactory factory) {
        super(db);
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
            writeComponents(Set.of(dag.getRootComponent()), visited, created, transaction);
            writeOutEdges(created.stream().flatMap(scc -> scc.nodes().stream()), transaction);
        });
    }

    private void writeComponents(Set<StrongComponent> components, Set<StrongComponent> visited,
                                 List<StrongComponent> created, TransactionContext transaction) {
        List<StrongComponent> unvisitedSCCs = components.stream().filter(visited::add).toList();
        if (unvisitedSCCs.isEmpty())
            return;

        Stream<GraphNode> firstNodes = unvisitedSCCs.stream().map(StrongComponent::getFirstNode);
        List<Boolean> createdNodes = writeFirstNodes(firstNodes, transaction);

        Set<GraphNode> restsToWrite = new HashSet<>();
        Set<StrongComponent> componentsToWrite = new HashSet<>();

        for (int i = 0; i < createdNodes.size(); i++) {
            if (createdNodes.get(i)) {
                StrongComponent scc = unvisitedSCCs.get(i);
                created.add(scc);
                restsToWrite.addAll(scc.getRestOfNodes());
                componentsToWrite.addAll(scc.targets());
            }
        }
        writeRestsOfNodes(restsToWrite.stream(), transaction);
        writeComponents(componentsToWrite, visited, created, transaction);
    }

    private List<Boolean> writeFirstNodes(Stream<GraphNode> nodes, TransactionContext transaction) {
        List<Map<String, Object>> nodesList = nodes.map(this::nodeToMap).toList();
        String query = "UNWIND $nodes AS node"
                + " OPTIONAL MATCH (h:Hashed {idHash: node.props.idHash})"
                + " WITH node, h IS NULL AS toCreate"
                + " FOREACH (_ IN CASE WHEN toCreate then [1] ELSE [] END |"
                + "  CREATE (n:$(node.label):Hashed)"
                + "  SET n = node.props"
                + " )"
                + " RETURN collect(toCreate) as created";
        return transaction.run(query, Map.of("nodes", nodesList))
                .single().get("created").asList(Value::asBoolean);
    }

    private void writeRestsOfNodes(Stream<GraphNode> nodes, TransactionContext transaction) {
        List<Map<String, Object>> nodesList = nodes.map(this::nodeToMap).toList();
        if (nodesList.isEmpty())
            return;

        String query = "FOREACH (node in $nodes |"
                + " CREATE (n:$(node.label):Hashed)"
                + " SET n = node.props"
                + ")";
        transaction.run(query, Map.of("nodes", nodesList));
    }

    private Map<String, Object> nodeToMap(GraphNode node) {
        Map<String, Object> properties = new HashMap<>();
        for (NodeProperty property : node.properties())
            properties.put(property.key(), property.value());
        properties.put("hash", node.hash().toString());
        properties.put("idHash", node.idHash().toString());

        return Map.of("label", node.label(), "props", properties);
    }

    private void writeOutEdges(Stream<GraphNode> nodes, TransactionContext transaction) {
        List<Map<String, Object>> edges = nodes.flatMap(this::getRelationships).toList();
        if (edges.isEmpty())
            return;

        String query = "UNWIND $edges AS edge"
                + " MATCH (from:Hashed {idHash: edge.from})"
                + " MATCH (to:Hashed {idHash: edge.to})"
                + " CALL apoc.create.relationship(from, edge.type, edge.props, to) YIELD rel"
                + " RETURN 0";

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
