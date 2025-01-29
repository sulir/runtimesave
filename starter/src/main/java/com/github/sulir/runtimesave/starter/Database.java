package com.github.sulir.runtimesave.starter;

import org.neo4j.driver.*;

import java.util.Map;

public class Database {
    public static final String URI = "bolt://localhost:7687";
    public static final String USER = "neo4j";
    public static final String PASSWORD = System.getenv("NEO4J_PASSWORD");
    public static final String DB_NAME = "runtimesave";
    private static Database instance;

    private final Driver driver;

    public static Database getInstance() {
        if (instance == null)
            instance = new Database();
        return instance;
    }

    private Database() {
        driver = GraphDatabase.driver(URI, AuthTokens.basic(USER, PASSWORD));
    }

    public String readPrimitiveVariable(String className, String method, String variable) {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
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
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
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
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
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
