package com.github.sulir.runtimesave.db;

import org.neo4j.driver.Session;

import java.util.Map;
import java.util.function.BooleanSupplier;

public class DbIndex {
    private static final Map<String, String> indexes = Map.of(
            "Class", "name",
            "Method", "signature",
            "Line", "number",
            "Frame", "hash"
    );

    private final DbConnection db;
    private BooleanSupplier shouldCancel;

    public DbIndex(DbConnection db) {
        this.db = db;
    }

    public boolean createIndexes() {
        createConstraint("Hashed", "idHash");

        for (Map.Entry<String, String> index : indexes.entrySet()) {
            if (shouldCancel != null && shouldCancel.getAsBoolean())
                return false;

            String label = index.getKey();
            String property = index.getValue();
            String query = "CREATE INDEX %1$s_%2$s IF NOT EXISTS FOR (n:%1$s) ON (n.%2$s)"
                    .formatted(label, property);
            try (Session session = db.createSession()) {
                session.run(query);
            }
        }

        return true;
    }

    public void createConstraint(String label, String property) {
        String query = "CREATE CONSTRAINT %1$s_%2$s IF NOT EXISTS FOR (n:%1$s) REQUIRE n.%2$s IS UNIQUE"
                .formatted(label, property);
        try (Session session = db.createSession()) {
            session.run(query);
        }
    }

    public void setCancellationListener(BooleanSupplier listener) {
        shouldCancel = listener;
    }
}
