package com.github.sulir.runtimesave.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GraphLoaderTest {
    private ObjectNode firstNode;
    private ObjectNode secondNode;
    private SampleClass first;
    private SampleClass second;

    @BeforeEach
    void setUp() {
        firstNode = new ObjectNode("id-1", SampleClass.class.getName());
        secondNode = new ObjectNode("id-2", SampleClass.class.getName());

        firstNode.setFields(Map.of(
                "number", new PrimitiveNode(1),
                "text", new StringNode("first"),
                "reference", secondNode
        ));
        secondNode.setFields(new HashMap<>(Map.of(
                "number", new PrimitiveNode(2),
                "text", new StringNode("second"),
                "reference", new NullNode()
        )));

        first = new SampleClass(1, "first");
        second = new SampleClass(2, "second");
        first.setReference(second);
    }

    @Test
    void simpleNodeTreeIsTransformedToObjects() {
        GraphLoader objectGraph = new GraphLoader(firstNode);
        SampleClass result = (SampleClass) objectGraph.createJavaObject();

        assertEquals(first, result);
        assertEquals(second, result.getReference());
        assertNull(result.getReference().getReference());
    }

    @Test
    void cyclicGraphIsTransformedToObjects() {
        secondNode.getFields().put("reference", firstNode);
        second.setReference(first);

        GraphLoader objectGraph = new GraphLoader(firstNode);
        SampleClass result = (SampleClass) objectGraph.createJavaObject();

        assertEquals(first, result);
        assertEquals(second, result.getReference());
        assertEquals(first, result.getReference().getReference());
    }
}