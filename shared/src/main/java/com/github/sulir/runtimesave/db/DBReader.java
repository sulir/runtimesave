package com.github.sulir.runtimesave.db;

import org.neo4j.driver.*;

import java.util.Map;

public class DBReader extends Database {
    private static DBReader instance;

    private DBReader() { }

    public static DBReader getInstance() {
        if (instance == null)
            instance = new DBReader();
        return instance;
    }

    public String readPrimitiveVariable(String className, String method, String variable) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})-->(:Method {signature: $method})-->(l:Line)"
                    + " WHERE l.number > 0"
                    + " WITH l ORDER BY l.number LIMIT 1"
                    + " MATCH (l)-[:HAS_VARIABLE {name: $variable}]->(v:Value)"
                    + " RETURN v.value";
            Result result = session.run(query, Map.of("class", className, "method", method,
                    "variable", variable));
            return result.next().values().get(0).asString();
        }
    }

    public String readStringVariable(String className, String method, String variable) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})-->(:Method {signature: $method})-->(l:Line)"
                    + " WHERE l.number > 0"
                    + " WITH l ORDER BY l.number LIMIT 1"
                    + " MATCH (l)-[:HAS_VARIABLE {name: $variable}]->(:Object)-[:HAS_VALUE]->(s:String)"
                    + " RETURN s.value";
            Result result = session.run(query, Map.of("class", className, "method", method,
                    "variable", variable));
            return result.next().values().get(0).asString();
        }
    }

    public String readObjectVariableId(String className, String method, String variable) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})-->(:Method {signature: $method})-->(l:Line)"
                    + " WHERE l.number > 0"
                    + " WITH l ORDER BY l.number LIMIT 1"
                    + " MATCH (l)-[:HAS_VARIABLE {name: $variable}]->(o:Object)"
                    + " RETURN elementId(o)";
            Result result = session.run(query, Map.of("class", className, "method", method,
                    "variable", variable));
            return result.next().values().get(0).asString();
        }
    }
}
