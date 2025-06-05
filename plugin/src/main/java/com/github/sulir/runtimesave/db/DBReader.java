package com.github.sulir.runtimesave.db;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

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

    public Record readVariable(SourceLocation location, String variable) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})-->(:Method {signature: $method})-->(l:Line {number: $line})"
                    + " MATCH (l)-[:HAS_VARIABLE {name: $variable}]->(v)"
                    + " OPTIONAL MATCH (v)-[:HAS_TYPE]->(t:Type)"
                    + " RETURN v, t";
            Result result = session.run(query, Map.of("class", location.className(),
                    "method", location.method(), "line", location.line(), "variable", variable));
            return result.next();
        }
    }

    public List<Record> readArrayElements(String id) {
        try (Session session = createSession()) {
            String query = "MATCH (a:Array)"
                    + " WHERE a.id = $id"
                    + " MATCH (a)-[e:HAS_ELEMENT]->(v)"
                    + " OPTIONAL MATCH (v)-[:HAS_TYPE]->(t:Type)"
                    + " RETURN e, v, t"
                    + " ORDER BY e.index";
            Result result = session.run(query, Map.of("id", id));
            return result.list();
        }
    }

    public List<Record> readObjectFields(String id) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Object)"
                    + " WHERE o.id = $id"
                    + " MATCH (o)-[f:HAS_FIELD]->(v)"
                    + " OPTIONAL MATCH (v)-[:HAS_TYPE]->(t:Type)"
                    + " RETURN f, v, t";
            Result result = session.run(query, Map.of("id", id));
            return result.list();
        }
    }
}
