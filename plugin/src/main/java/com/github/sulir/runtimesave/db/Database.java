package com.github.sulir.runtimesave.db;

import org.neo4j.driver.*;

public abstract class Database {
    public static final String URI = "bolt://localhost:7687";
    public static final String USER = "neo4j";
    public static final String PASSWORD = System.getenv("NEO4J_PASSWORD");
    public static final String DB_NAME = "runtimesave";

    private final Driver driver;

    protected Database() {
        driver = GraphDatabase.driver(URI, AuthTokens.basic(USER, PASSWORD));
    }

    protected Session createSession() {
        return driver.session(SessionConfig.forDatabase(DB_NAME));
    }
}
