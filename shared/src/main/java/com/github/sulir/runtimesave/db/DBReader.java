package com.github.sulir.runtimesave.db;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;

import java.util.List;
import java.util.Map;

public class DBReader extends Database {
    private static DBReader instance;

    private DBReader() { }

    public static DBReader getInstance() {
        if (instance == null)
            instance = new DBReader();
        return instance;
    }

    public Node readVariable(String className, String method, String variable) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})-->(:Method {signature: $method})-->(l:Line)"
                    + " WHERE l.number > 0"
                    + " WITH l ORDER BY l.number LIMIT 1"
                    + " MATCH (l)-[:HAS_VARIABLE {name: $variable}]->(v)"
                    + " RETURN v";
            Result result = session.run(query, Map.of("class", className, "method", method,
                    "variable", variable));
            return result.next().get("v").asNode();
        }
    }

    public List<Record> readObjectFields(String id) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Object)"
                    + " WHERE elementId(o) = $id"
                    + " MATCH (o)-[f:HAS_FIELD]->(n)"
                    + " RETURN f, n";
            Result result = session.run(query, Map.of("id", id));
            return result.list();
        }
    }
}
