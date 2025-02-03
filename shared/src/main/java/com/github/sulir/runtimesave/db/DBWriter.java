package com.github.sulir.runtimesave.db;

import org.neo4j.driver.*;

import java.util.Map;

public class DBWriter extends Database {
    private static DBWriter instance;

    private DBWriter() { }

    public static DBWriter getInstance() {
        if (instance == null)
            instance = new DBWriter();
        return instance;
    }

    public void writeLocation(SourceLocation location) {
        try (Session session = createSession()) {
            String query = "MERGE (c:Class {name: $class})"
                    + " MERGE (c)-[:DEFINES]->(m:Method {signature: $method})"
                    + " MERGE (m)-[:CONTAINS]->(:Line {number: $line})";
            session.run(query, Map.of("class", location.getClassName(), "method", location.getMethod(),
                    "line", location.getLine()));
        }
    }

    public void writePrimitiveVariable(SourceLocation location, String name, String type, Object value) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(:Value {type: $type, value: $value})";
            session.run(query, Map.of("class", location.getClassName(), "method", location.getMethod(),
                    "line", location.getLine(), "name", name, "type", type, "value", value));
        }
    }

    public void writeStringVariable(SourceLocation location, String name, String value) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " MERGE (s:String {value: $value})"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(s)";
            session.run(query, Map.of("class", location.getClassName(), "method", location.getMethod(),
                    "line", location.getLine(), "name", name, "value", value));
        }
    }

    public boolean writeObjectVariable(SourceLocation location, String name, String type, long jvmId) {
        try (Session session = createSession()) {
            String createOrMerge = jvmId == -1 ? " CREATE (o:Object {jvmId: $jvmId, type: $type})"
                    : " MERGE (o:Object {jvmId: $jvmId}) ON CREATE SET o.id = randomUUID(), o.type = $type";
            String query = "MERGE (s:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + createOrMerge
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(o)";
            Result result = session.run(query, Map.of("class", location.getClassName(), "method", location.getMethod(),
                    "line", location.getLine(), "name", name, "type", type, "jvmId", jvmId));

            return result.consume().counters().nodesCreated() > 0;
        }
    }

    public void writePrimitiveField(long jvmId, String name, String type, Object value) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Object {jvmID: $jvmId})"
                    + " CREATE (o)-[:HAS_FIELD {name: $name}]->(:Value {type: $type, value: $value})";
            session.run(query, Map.of("jvmId", jvmId, "name", name, "type", type, "value", value));
        }
    }

    public boolean writeObjectField(long parentId, String name, String type, long childId) {
        try (Session session = createSession()) {
            String createOrMerge = childId == -1 ? " CREATE (c:Object {jvmId: $childId, type: $type})"
                    : " MERGE (c:Object {jvmId: $childId}) ON CREATE SET c.id = randomUUID(), c.type = $type";
            String query = "MATCH (p:Object {jvmId: $parentId})"
                    + createOrMerge
                    + " CREATE (p)-[:HAS_FIELD {name: $name}]->(c)";
            Result result = session.run(query, Map.of("parentId", parentId, "name", name, "type", type,
                    "childId", childId));

            return result.consume().counters().nodesCreated() > 0;
        }
    }

    public void writeStringField(long jvmId, String name, String value) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Object {jvmId: $jvmId})"
                    + " MERGE (s:String {value: $value})"
                    + " CREATE (o)-[:HAS_FIELD {name: $name}]->(s)";
            session.run(query, Map.of("jvmId", jvmId, "name", name, "value", value));
        }
    }

    public void deleteJvmIds() {
        try (Session session = createSession()) {
            session.run("MATCH (o:Object) REMOVE o.jvmId");
        }
    }
}
