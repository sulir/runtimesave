package com.github.sulir.runtimesave.db;

import org.neo4j.driver.*;

public class Database {
    private static final String URI = "bolt://localhost:7687";
    private static final String USER = "neo4j";
    private static final String PASSWORD = System.getenv("NEO4J_PASSWORD");
    private static final String DB_NAME = "runtimesave";
    private static Database instance;

    private final Driver driver;

    public static Database getInstance() {
        if (instance == null)
            instance = new Database();
        return instance;
    }

    protected Database() {
        driver = GraphDatabase.driver(URI, AuthTokens.basic(USER, PASSWORD));
    }

    public Session createSession() {
        return driver.session(SessionConfig.forDatabase(DB_NAME));
    }
}
