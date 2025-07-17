package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.graph.NodeFactory;
import org.neo4j.driver.Session;

public abstract class Database {
    protected final DbConnection db;
    protected final NodeFactory factory;

    public Database(DbConnection db, NodeFactory factory) {
        this.db = db;
        this.factory = factory;
    }

    public void deleteAll() {
        String labelPattern = String.join("|", factory.getLabels());
        try (Session session = db.createSession()) {
            session.run("MATCH (n:" + labelPattern + ") DETACH DELETE n");
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
