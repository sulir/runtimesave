package com.github.sulir.runtimesave.graph;

import org.neo4j.driver.Record;

public class StringNode extends GraphNode {
    private final String value;

    public static StringNode fromDB(Record record) {
        return new StringNode(record.get("v").get("value").asString());
    }

    public StringNode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
