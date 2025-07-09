package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.nodes.NullNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.packing.Packer;

public class SparseArrayPacker implements Packer {
    private static final int MIN_PACKED_ELEMENTS = 1;

    @Override
    public boolean canPack(ValueNode node) {
        return node instanceof ArrayNode;
    }

    @Override
    public ValueNode pack(ValueNode node) {
        ArrayNode array = (ArrayNode) node;
        SparseArrayNode sparseArray = new SparseArrayNode(array.getType(), array.getLength());
        Object defaultValue = getDefaultValue(array.getComponentType());

        array.forEachElement((index, value) -> {
            if (value instanceof NullNode)
                return;
            if (value instanceof PrimitiveNode primitive && primitive.getValue().equals(defaultValue))
                return;
            sparseArray.setElement(index, value);
        });
        int removedCount = array.edgeCount() - sparseArray.edgeCount();
        return removedCount >= MIN_PACKED_ELEMENTS ? sparseArray : array;
    }

    @Override
    public boolean canUnpack(ValueNode node) {
        return node instanceof SparseArrayNode;
    }

    @Override
    public ValueNode unpack(ValueNode node) {
        SparseArrayNode sparseArray = (SparseArrayNode) node;
        String componentType = sparseArray.getComponentType();
        Object defaultValue = getDefaultValue(componentType);
        ArrayNode array = new ArrayNode(sparseArray.getType());

        for (int i = 0; i < sparseArray.getLength(); i++) {
            ValueNode value = sparseArray.getElement(i);
            if (value == null) {
                if (defaultValue == null)
                    array.setElement(i, NullNode.getInstance());
                else
                    array.setElement(i, new PrimitiveNode(defaultValue, componentType));
            } else {
                array.setElement(i, value);
            }
        }
        return array;
    }

    public static class SparseArrayNode extends ArrayNode {
        private static final Mapping mapping = mapping(SparseArrayNode.class)
                .property("type", String.class, SparseArrayNode::getType)
                .property("length", Integer.class, ArrayNode::getLength)
                .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
                .argsConstructor(props -> new SparseArrayNode((String) props[0], (int) props[1]));

        private final int length;

        public SparseArrayNode(String type, int length) {
            super(type);
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public Mapping getMapping() {
            return mapping;
        }
    }

    private Object getDefaultValue(String type) {
        return switch (type) {
            case "char" -> '\u0000';
            case "byte" -> (byte) 0;
            case "short" -> (short) 0;
            case "int" -> 0;
            case "long" -> 0L;
            case "float" -> 0.0f;
            case "double" -> 0.0;
            case "boolean" -> false;
            default -> null;
        };
    }
}
