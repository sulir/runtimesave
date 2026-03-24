package com.github.sulir.runtimesave.db;

import org.neo4j.driver.*;

import java.util.function.Consumer;

public class DbConnection {
    private static final String URI = "bolt://localhost:7687";
    private static final String USER = "neo4j";
    private static final String PASSWORD = System.getenv("NEO4J_PASSWORD");
    private static final String DB_NAME = "runtimesave";
    private static DbConnection instance;

    private final Driver driver;
    private final String dbName;

    public static DbConnection getInstance() {
        if (instance == null)
            instance = new DbConnection(URI, USER, PASSWORD, DB_NAME);
        return instance;
    }

    public DbConnection(String uri, String user, String password, String dbName) {
        Config config = Config.builder().withMinimumNotificationSeverity(NotificationSeverity.OFF).build();
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
        this.dbName = dbName;
    }

    public Session createSession() {
        return driver.session(SessionConfig.forDatabase(dbName));
    }

    public void writeTransaction(Consumer<TransactionContext> action) {
        try (Session session = createSession()) {
            session.executeWriteWithoutResult(action);
        }
    }
}
