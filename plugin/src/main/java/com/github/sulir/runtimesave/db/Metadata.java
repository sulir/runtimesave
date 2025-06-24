package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.MismatchException;
import com.github.sulir.runtimesave.SourceLocation;
import com.github.sulir.runtimesave.hash.NodeHash;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.NoSuchRecordException;

import java.util.Map;

public class Metadata {
    private final DbConnection db;

    public Metadata(DbConnection db) {
        this.db = db;
    }

    public NodeHash findFrame(SourceLocation location) throws MismatchException {
        try (Session session = db.createSession()) {
            String query = "MATCH (:Class {name: $class})"
                    + "-->(:Method {signature: $method})"
                    + "-->(l:Line {number: $line})"
                    + "-->(f:Frame)"
                    + " RETURN f.hash AS frameHash";
            Result result = session.run(query, Map.of("class", location.className(), "method", location.method(),
                "line", location.line()));

            return NodeHash.fromString(result.single().get("frameHash").asString());
        } catch (NoSuchRecordException e) {
            throw new MismatchException("No or multiple frames found for " + location);
        }
    }

    public void addLocation(NodeHash frameHash, SourceLocation location) {
        try (Session session = db.createSession()) {
            String query = "MATCH (f:Frame) WHERE f.hash = $hash"
                    + " MERGE (c:Class {name: $class})"
                    + " MERGE (c)-[:DEFINES]->(m:Method {signature: $method})"
                    + " MERGE (m)-[:CONTAINS]->(l:Line {number: $line})"
                    + " MERGE (l)-[:HAS_FRAME]->(f)";
            session.run(query, Map.of("hash", frameHash.toString(), "class", location.className(),
                "method", location.method(), "line", location.line()));
        }
    }
}
