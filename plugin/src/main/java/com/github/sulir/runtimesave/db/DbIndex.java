package com.github.sulir.runtimesave.db;

import org.neo4j.driver.Session;

import java.util.function.BooleanSupplier;

public class DbIndex {
    private final DbConnection db;
    private BooleanSupplier shouldCancel;

    public DbIndex(DbConnection db) {
        this.db = db;
    }

    public boolean createIndexes() {
        if (shouldCancel != null && shouldCancel.getAsBoolean())
            return false;

        String query = "CREATE INDEX hashed_id_hash IF NOT EXISTS FOR (n:Hashed) ON (n.idHash)";
        try (Session session = db.createSession()) {
            session.run(query);
        }

        return true;
    }

    public void setCancellationListener(BooleanSupplier listener) {
        shouldCancel = listener;
    }
}
