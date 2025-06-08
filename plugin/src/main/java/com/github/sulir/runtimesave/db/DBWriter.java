package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.graph.SourceLocation;
import org.neo4j.driver.*;

import java.util.Map;

public class DBWriter extends Database {
    private static final String STRING_TYPE = "java.lang.String";
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
            session.run(query, Map.of("class", location.className(), "method", location.method(),
                    "line", location.line()));
        }
    }

    public void writePrimitiveVariable(SourceLocation location, String name, String type, Object value) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (p:Primitive {value: $value})-[:HAS_TYPE]->(t)"
                    + " ON CREATE SET p.id = randomUUID()"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(p)";
            session.run(query, Map.of("class", location.className(), "method", location.method(),
                    "line", location.line(), "name", name, "type", type, "value", value));
        }
    }

    public void writeNullVariable(SourceLocation location, String name) {
        try (Session session = createSession()) {
            String query = "MERGE (:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " MERGE (n:Null)"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(n)";
            session.run(query, Map.of("class", location.className(), "method", location.method(),
                    "line", location.line(), "name", name));
        }
    }

    public void writeStringVariable(SourceLocation location, String name, String value) {
        try (Session session = createSession()) {
            String query = "MATCH (:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " MERGE (s:String {value: $value})"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(s)"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (s)-[:HAS_TYPE]->(t)";
            session.run(query, Map.of("class", location.className(), "method", location.method(),
                    "line", location.line(), "name", name, "value", value, "type", STRING_TYPE));
        }
    }

    public boolean writeObjectVariable(SourceLocation location, String name, String type, long jvmId) {
        try (Session session = createSession()) {
            String query = "MERGE (:Class {name: $class})"
                    + "-[:DEFINES]->(:Method {signature: $method})"
                    + "-[:CONTAINS]->(l:Line {number: $line})"
                    + " MERGE (o:" + getNodeLabel(type) + " {jvmId: $jvmId})"
                    + " ON CREATE SET o.id = randomUUID()"
                    + " CREATE (l)-[:HAS_VARIABLE {name: $name}]->(o)"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (o)-[:HAS_TYPE]->(t)";
            Result result = session.run(query, Map.of("class", location.className(), "method", location.method(),
                    "line", location.line(), "name", name, "type", type, "jvmId", jvmId));

            return result.consume().counters().nodesCreated() > 0;
        }
    }

    public void writePrimitiveField(long jvmId, String name, String type, Object value) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Object {jvmId: $jvmId})"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (p:Primitive {value: $value})-[:HAS_TYPE]->(t)"
                    + " ON CREATE SET p.id = randomUUID()"
                    + " CREATE (o)-[:HAS_FIELD {name: $name}]->(p)";
            session.run(query, Map.of("jvmId", jvmId, "name", name, "type", type, "value", value));
        }
    }

    public void writeNullField(long jvmId, String name) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Object {jvmId: $jvmId})"
                    + " MERGE (n:Null)"
                    + " CREATE (o)-[:HAS_FIELD {name: $name}]->(n)";
            session.run(query, Map.of("jvmId", jvmId, "name", name));
        }
    }

    public void writeStringField(long jvmId, String name, String value) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Object {jvmId: $jvmId})"
                    + " MERGE (s:String {value: $value})"
                    + " CREATE (o)-[:HAS_FIELD {name: $name}]->(s)"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (s)-[:HAS_TYPE]->(t)";
            session.run(query, Map.of("jvmId", jvmId, "name", name, "value", value, "type", STRING_TYPE));
        }
    }

    public boolean writeObjectField(long parentId, String name, String type, long childId) {
        try (Session session = createSession()) {
            String query = "MATCH (p:Object {jvmId: $parentId})"
                    + " MERGE (c:" + getNodeLabel(type) + " {jvmId: $childId})"
                    + " ON CREATE SET c.id = randomUUID()"
                    + " CREATE (p)-[:HAS_FIELD {name: $name}]->(c)"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (c)-[:HAS_TYPE]->(t)";
            Result result = session.run(query, Map.of("parentId", parentId, "name", name, "type", type,
                    "childId", childId));

            return result.consume().counters().nodesCreated() > 0;
        }
    }

    public void writePrimitiveElement(long jvmId, int index, String type, Object value) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Array {jvmId: $jvmId})"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (p:Primitive {value: $value})-[:HAS_TYPE]->(t)"
                    + " ON CREATE SET p.id = randomUUID()"
                    + " CREATE (o)-[:HAS_ELEMENT {index: $index}]->(p)";
            session.run(query, Map.of("jvmId", jvmId, "index", index, "type", type, "value", value));
        }
    }

    public void writeNullElement(long jvmId, int index) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Array {jvmId: $jvmId})"
                    + " MERGE (n:Null)"
                    + " CREATE (o)-[:HAS_ELEMENT {index: $index}]->(n)";
            session.run(query, Map.of("jvmId", jvmId, "index", index));
        }
    }

    public void writeStringElement(long jvmId, int index, String value) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Array {jvmId: $jvmId})"
                    + " MERGE (s:String {value: $value})"
                    + " CREATE (o)-[:HAS_ELEMENT {index: $index}]->(s)"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (s)-[:HAS_TYPE]->(t)";
            session.run(query, Map.of("jvmId", jvmId, "index", index, "value", value, "type", STRING_TYPE));
        }
    }

    public boolean writeObjectElement(long jvmId, int index, String type, long childId) {
        try (Session session = createSession()) {
            String query = "MATCH (o:Array {jvmId: $jvmId})"
                    + " MERGE (c:" + getNodeLabel(type) + " {jvmId: $childId})"
                    + " ON CREATE SET c.id = randomUUID()"
                    + " CREATE (o)-[:HAS_ELEMENT {index: $index}]->(c)"
                    + " MERGE (t:Type {name: $type})"
                    + " MERGE (c)-[:HAS_TYPE]->(t)";
            Result result = session.run(query, Map.of("jvmId", jvmId, "index", index, "type", type, "childId", childId));

            return result.consume().counters().nodesCreated() > 0;
        }
    }

    public void deleteJvmIds() {
        try (Session session = createSession()) {
            session.run("MATCH (o:Object|Array) REMOVE o.jvmId");
        }
    }

    private String getNodeLabel(String type) {
        if (type.contains("[")) {
            return "Array";
        } else {
            return "Object";
        }
    }
}
