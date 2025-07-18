package com.github.sulir.runtimesave.db;

import org.neo4j.driver.Session;

public abstract class Database {
    protected final DbConnection db;

    public Database(DbConnection db) {
        this.db = db;
    }

    public void deleteAll() {
        try (Session session = db.createSession()) {
            session.run("MATCH (n:Hashed|Meta|Node) DETACH DELETE n");
        }
    }

    public int nodeCount() {
        try (Session session = db.createSession()) {
            return session.run("MATCH (n) RETURN COUNT(n) AS count").single().get("count").asInt();
        }
    }

    public int edgeCount() {
        try (Session session = db.createSession()) {
            return session.run("MATCH ()-[r]-() RETURN COUNT(r) AS count").single().get("count").asInt();
        }
    }
}
