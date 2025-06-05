package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.db.DBReader;
import org.neo4j.driver.Record;

import java.util.List;

public class ArrayNode extends ReferenceNode {
    private GraphNode[] elements;

    public ArrayNode(String id, String type) {
        super(id, type);
    }

    public GraphNode[] getElements() {
        if (elements == null)
            loadElements();

        return elements;
    }

    private void loadElements() {
        List<Record> elements = DBReader.getInstance().readArrayElements(getId());
        this.elements = new GraphNode[elements.size()];

        for (Record record : elements) {
            int index = record.get("e").get("index").asInt();
            this.elements[index] = GraphNode.fromDB(record);
        }
    }
}
