package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.NullNode;
import com.github.sulir.runtimesave.nodes.ObjectNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class LinkedListPacker implements Packer {
    private static final String LIST_TYPE = "java.util.LinkedList";
    private static final String LIST_NODE_TYPE = "java.util.LinkedList$Node";

    @Override
    public boolean canPack(ValueNode node) {
        return node instanceof ObjectNode object && object.getType().equals(LIST_TYPE);
    }

    @Override
    public ValueNode pack(ValueNode node) {
        ObjectNode list = (ObjectNode) node;
        int modCount = (int) ((PrimitiveNode) list.getField("modCount")).getValue();
        LinkedListNode result = new LinkedListNode(modCount);
        ValueNode listNode = list.getField("first");
        while (listNode instanceof ObjectNode listNodeObject) {
            ValueNode item = listNodeObject.getField("item");
            result.addElement(item);
            listNode = listNodeObject.getField("next");
        }
        return result;
    }

    @Override
    public boolean canUnpack(ValueNode node) {
        return node instanceof LinkedListNode;
    }

    @Override
    public ValueNode unpack(ValueNode node) {
        LinkedListNode list = (LinkedListNode) node;
        ObjectNode result = new ObjectNode(LIST_TYPE);
        result.setField("modCount", new PrimitiveNode(list.getModCount(), "int"));
        result.setField("size", new PrimitiveNode(list.size(), "int"));

        result.setField("first", NullNode.getInstance());
        result.setField("last", NullNode.getInstance());

        list.getElements().forEach(item -> {
            ObjectNode newNode = new ObjectNode(LIST_NODE_TYPE);
            newNode.setField("item", item);
            newNode.setField("next", NullNode.getInstance());
            newNode.setField("prev", NullNode.getInstance());

            if (result.getField("first") instanceof NullNode) {
                result.setField("first", newNode);
                result.setField("last", newNode);
            } else {
                ObjectNode last = (ObjectNode) result.getField("last");
                last.setField("next", newNode);
                newNode.setField("prev", last);
                result.setField("last", newNode);
            }
        });

        return result;
    }

    public static class LinkedListNode extends ValueNode {
        private static final Mapping mapping = mapping(LinkedListNode.class)
                .property("modCount", int.class, LinkedListNode::getModCount)
                .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
                .constructor(LinkedListNode::new);

        private final int modCount;
        private final SortedMap<Integer, ValueNode> elements = new TreeMap<>();

        public LinkedListNode(int modCount) {
            this.modCount = modCount;
        }

        public int getModCount() {
            return modCount;
        }

        public Stream<ValueNode> getElements() {
            return elements.values().stream();
        }

        public void addElement(ValueNode element) {
            elements.put(elements.size(), element);
        }

        public int size() {
            return elements.size();
        }

        @Override
        public Mapping getMapping() {
            return mapping;
        }
    }
}
