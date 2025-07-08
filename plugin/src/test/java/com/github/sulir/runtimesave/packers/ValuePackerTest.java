package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValuePackerTest {
    private FrameNode frame;
    private ObjectNode ignored;
    private ObjectNode packable;
    private ObjectNode subPackable;

    @BeforeEach
    void setUp() {
        frame = new FrameNode();
        ignored = new ObjectNode("Ignored");
        packable = new ObjectNode("Packable");
        subPackable = new ObjectNode("SubPackable");

        frame.setVariable("ignored", ignored);
        ObjectNode subIgnored = new ObjectNode("SubIgnored");
        ignored.setField("ref1", subIgnored);
        packable.setField("ref2", new StringNode(""));
        subIgnored.setField("cycle", ignored);
    }

    @Test
    void packingNodesWithCyclesReplacesSourceEdges() {
        frame.setVariable("var", packable);
        packable.setField("ref1", subPackable);
        subPackable.setField("ref2", packable);

        ValuePacker packer = new ValuePacker(new Packer[]{new TestPacker()});
        packer.pack(frame);
        assertOnlyTypeChanged(frame.getVariable("var"), "Replacement");
        assertEquals(ignored, frame.getVariable("ignored"));
    }

    @Test
    void unpackingNodesWithCyclesRestoresSourceEdges() {
        ObjectNode replacement = new ObjectNode("Replacement");
        frame.setVariable("var", replacement);
        replacement.setField("ref1", subPackable);
        subPackable.setField("ref2", replacement);

        ValuePacker packer = new ValuePacker(new Packer[]{new TestPacker()});
        packer.unpack(frame);
        assertOnlyTypeChanged(frame.getVariable("var"), "Packable");
        assertEquals(ignored, frame.getVariable("ignored"));
    }

    @Test
    void packingValueReturnsReplacement() {
        packable.setField("ref1", subPackable);
        subPackable.setField("ref2", packable);

        ValuePacker packer = new ValuePacker(new Packer[]{new TestPacker()});
        assertOnlyTypeChanged(packer.pack(packable), "Replacement");
    }

    @Test
    void unpackingValueReturnsOriginal() {
        ObjectNode replacement = new ObjectNode("Replacement");
        replacement.setField("ref1", subPackable);
        subPackable.setField("ref2", replacement);

        ValuePacker packer = new ValuePacker(new Packer[]{new TestPacker()});
        assertOnlyTypeChanged(packer.unpack(replacement), "Packable");
    }

    @Test
    void loadingPackersFromServiceLoaderDoesNotCrash() {
        assertDoesNotThrow(ValuePacker::fromServiceLoader);
    }

    private void assertOnlyTypeChanged(GraphNode node, String type) {
        assertInstanceOf(ObjectNode.class, node);
        ObjectNode changed = (ObjectNode) node;
        assertEquals(type, changed.getType());
        assertEquals(subPackable, changed.getField("ref1"));
        assertEquals(changed, subPackable.getField("ref2"));
    }

    private static class TestPacker implements Packer {
        @Override
        public boolean canPack(ValueNode node) {
            return node instanceof ObjectNode object && object.getType().equals("Packable");
        }

        @Override
        public ValueNode pack(ValueNode node) {
            ObjectNode object = (ObjectNode) node;
            ObjectNode replacement = new ObjectNode("Replacement");
            for (String field : object.getFieldNames())
                replacement.setField(field, object.getField(field));
            return replacement;
        }

        @Override
        public boolean canUnpack(ValueNode node) {
            return node instanceof ObjectNode object && object.getType().equals("Replacement");
        }

        @Override
        public ValueNode unpack(ValueNode node) {
            ObjectNode object = (ObjectNode) node;
            ObjectNode packable = new ObjectNode("Packable");
            for (String field : object.getFieldNames())
                packable.setField(field, object.getField(field));
            return packable;
        }
    }
}