package com.github.sulir.runtimesave;

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

    public void writeLocation(SourceLocation location) {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
            String query = "MERGE (c:Class {name: $class})"
                    + " MERGE (c)-[:DEFINES]->(m:Method {signature: $method})"
                    + " MERGE (m)-[:CONTAINS]->(:Line {number: $line})";
            session.run(query, Map.of("class", location.getClassName(), "method", location.getMethod(),
                    "line", location.getLine()));
        }
    }

    public void writePrimitiveVariable(SourceLocation location, String name, String type, String value) {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
            String query = "MATCH (:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(:Value {type: $type, value: $value})";
            session.run(query, Map.of("class", location.getClassName(), "method", location.getMethod(),
                    "line", location.getLine(), "name", name, "type", type, "value", value));
        }
    }

    public boolean writeObjectVariable(SourceLocation location, String name, String type, long objectID) {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
            String mode = objectID == -1 ? "CREATE" : "MERGE";
            String query = "MERGE (s:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " " + mode + " (o:Object {type: $type, objectID: $id})"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(o)";
            Result result = session.run(query, Map.of("class", location.getClassName(), "method", location.getMethod(),
                    "line", location.getLine(), "name", name, "type", type, "id", objectID));

            return result.consume().counters().nodesCreated() > 0;
        }
    }

    public void writePrimitiveField(long objectID, String name, String type, String value) {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
            String query = "MATCH (o:Object {objectID: $id})"
                    + " CREATE (o)-[:HAS_FIELD {name: $name}]->(:Value {type: $type, value: $value})";
            session.run(query, Map.of("id", objectID, "name", name, "type", type, "value", value));
        }
    }

    public boolean writeObjectField(long parentID, String name, String type, long childID) {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
            String mode = childID == -1 ? "CREATE" : "MERGE";
            String query = "MATCH (p:Object {objectID: $parent})"
                    + " " + mode + " (c:Object {type: $type, objectID: $child})"
                    + " CREATE (p)-[:HAS_FIELD {name: $name}]->(c)";
            Result result = session.run(query, Map.of("parent", parentID, "name", name, "type", type,
                    "child", childID));

            return result.consume().counters().nodesCreated() > 0;
        }
    }

    public void writeString(long objectID, String value) {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
            String query = "MATCH (o:Object {objectID: $id})"
                    + " MERGE (s:String {value: $value})"
                    + " CREATE (o)-[:HAS_VALUE]->(s)";
            session.run(query, Map.of("id", objectID, "value", value));
        }
    }

    public void deleteObjectIDs() {
        try (Session session = driver.session(SessionConfig.forDatabase(DB_NAME))) {
            session.run("MATCH (o:Object) REMOVE o.objectID");
        }
    }
}
