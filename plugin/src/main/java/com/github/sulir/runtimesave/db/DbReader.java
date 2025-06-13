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
            for (Node node : nodes) {
                String label = node.labels().iterator().next();
                idToNode.put(node.elementId(), ObjectMapper.getInstance(label).createNode(node));
            }

            for (Relationship edge : edges) {
                GraphNode from = idToNode.get(edge.startNodeElementId());
                GraphNode to = idToNode.get(edge.endNodeElementId());
                ObjectMapper.getInstance(from.getClass()).createEdge(from, to, edge);
            }

            return type.cast(idToNode.get(elementId));
        }
    }
}
