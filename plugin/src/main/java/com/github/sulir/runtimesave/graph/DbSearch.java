package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.Database;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.NoSuchRecordException;

import java.util.Map;

public class DbSearch {
    private final Database db;

    public DbSearch(Database db) {
        this.db = db;
    }

    public String findFrame(SourceLocation location) throws MismatchException {
        try (Session session = db.createSession()) {
            String query = "MATCH (:Class {name: $class})-->(:Method {signature: $method})-->(l:Line {number: $line})"
                    + " RETURN elementId(l)";
            Result result = session.run(query, Map.of("class", location.className(), "method", location.method(),
                "line", location.line()));

            return result.single().get(0).asString();
        } catch (NoSuchRecordException e) {
            throw new MismatchException(String.format("No or multiple frames found for %s.%s:%d",
                location.className(), location.method(), location.line()));
        }
    }
}
