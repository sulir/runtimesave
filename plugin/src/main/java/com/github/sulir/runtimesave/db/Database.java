package com.github.sulir.runtimesave.db;

import org.neo4j.driver.*;

public class Database {
    private static final String URI = "bolt://localhost:7687";
    private static final String USER = "neo4j";
    private static final String PASSWORD = System.getenv("NEO4J_PASSWORD");
    private static final String DB_NAME = "runtimesave";
    private static Database instance;

    private final Driver driver;
    private final String dbName;

    public static Database getInstance() {
        if (instance == null)
            instance = new Database(URI, USER, PASSWORD, DB_NAME);
        return instance;
    }

    public Database(String uri, String user, String password, String dbName) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        this.dbName = dbName;
    }

    public Session createSession() {
        return driver.session(SessionConfig.forDatabase(dbName));
    }
}
