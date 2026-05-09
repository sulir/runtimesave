package io.github.sulir.runtimesave.db;

import org.neo4j.driver.Session;

import java.util.Map;

public class DbIndex {
    private static final Map<String, String> indexes = Map.of(
            "Class", "name",
            "Method", "signature",
            "Line", "number",
            "Hashed", "hash"
    );

    private final DbConnection db;

    public DbIndex(DbConnection db) {
        this.db = db;
    }

    public void createIndexes() {
        createConstraint();

        for (Map.Entry<String, String> index : indexes.entrySet()) {
            String label = index.getKey();
            String property = index.getValue();
            String query = "CREATE INDEX %1$s_%2$s IF NOT EXISTS FOR (n:%1$s) ON (n.%2$s)"
                    .formatted(label, property);
            try (Session session = db.createSession()) {
                session.run(query);
            }
        }
    }

    private void createConstraint() {
        String query = "CREATE CONSTRAINT %1$s_%2$s IF NOT EXISTS FOR (n:%1$s) REQUIRE n.%2$s IS UNIQUE"
                .formatted("Hashed", "idHash");
        try (Session session = db.createSession()) {
            session.run(query);
        }
    }
}
