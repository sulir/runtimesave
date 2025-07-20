package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.nodes.NullNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.pack.Packer;

import java.util.SortedMap;
import java.util.TreeMap;

public class SparseArrayPacker implements Packer {
    @Override
    public boolean canPack(ValueNode node) {
        return node instanceof ArrayNode array && array.getLength() != 0;
    }

    @Override
    public ValueNode pack(ValueNode node) {
        ArrayNode array = (ArrayNode) node;
        SparseArrayNode sparseArray = new SparseArrayNode(array.getType(), array.getLength());
        Object defaultValue = getDefaultValue(array.getComponentType());

        array.forEachElement((index, value) -> {
            boolean isDefaultPrimitive = value instanceof PrimitiveNode primitive
                    && primitive.getValue().equals(defaultValue);
            if (!(value instanceof NullNode || isDefaultPrimitive))
                sparseArray.setElement(index, value);
        });
        return sparseArray;
    }

    @Override
    public boolean canUnpack(ValueNode node) {
        return node instanceof SparseArrayNode;
    }

    @Override
    public ValueNode unpack(ValueNode node) {
        SparseArrayNode sparseArray = (SparseArrayNode) node;
        ArrayNode array = new ArrayNode(sparseArray.getType());

        for (int i = 0; i < sparseArray.getLength(); i++)
            array.setElement(i, sparseArray.getElement(i));

        return array;
    }

    public static class SparseArrayNode extends ValueNode {
        public static final Mapping mapping = mapping(SparseArrayNode.class)
                .property("type", String.class, SparseArrayNode::getType)
                .property("length", Integer.class, SparseArrayNode::getLength)
                .edges("HAS_ELEMENT", "index", Integer.class, ValueNode.class, node -> node.elements)
                .argsConstructor(props -> new SparseArrayNode((String) props[0], (int) props[1]));

        private final String type;
        private final int length;
        protected final SortedMap<Integer, ValueNode> elements = new TreeMap<>();

        public SparseArrayNode(String type, int length) {
            if (!type.endsWith("[]"))
                throw new IllegalArgumentException("Invalid sparse array type: " + type);
            this.type = type;
            this.length = length;
        }

        public String getType() {
            return type;
        }

        public String getComponentType() {
            return type.substring(0, type.length() - 2);
        }

        public int getLength() {
            return length;
        }

        public ValueNode getElement(int index) {
            ValueNode value = elements.get(index);
            if (value != null)
                return value;

            String componentType = getComponentType();
            Object defaultValue = getDefaultValue(componentType);
            if (defaultValue == null)
                return NullNode.getInstance();
            else
                return new PrimitiveNode(defaultValue, componentType);
        }

        public void setElement(int index, ValueNode value) {
            checkModification();
            elements.put(index, value);
        }

        @Override
        public Mapping getMapping() {
            return mapping;
        }
    }

    private static Object getDefaultValue(String type) {
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
