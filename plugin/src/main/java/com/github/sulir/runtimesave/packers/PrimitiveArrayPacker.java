package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.nodes.ArrayNode;
import com.github.sulir.runtimesave.nodes.PrimitiveNode;
import com.github.sulir.runtimesave.pack.Packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrimitiveArrayPacker implements Packer {
    @Override
    public boolean canPack(ValueNode node) {
        return node instanceof ArrayNode array && array.getLength() != 0 && isPrimitive(array.getComponentType());
    }

    @Override
    public ValueNode pack(ValueNode node) {
        ArrayNode array = (ArrayNode) node;
        List<Object> values = new ArrayList<>(array.getLength());
        array.forEachElement((index, target) -> values.add(index, ((PrimitiveNode) target).getValue()));
        return new PrimitiveArrayNode(array.getType(), values);
    }

    @Override
    public boolean canUnpack(ValueNode node) {
        return node instanceof PrimitiveArrayNode;
    }

    @Override
    public ValueNode unpack(ValueNode node) {
        PrimitiveArrayNode primitiveArray = (PrimitiveArrayNode) node;
        ArrayNode array = new ArrayNode(primitiveArray.getType());
        String componentType = primitiveArray.getComponentType();
        primitiveArray.getElements().forEach(value -> array.addElement(new PrimitiveNode(value, componentType)));
        return array;
    }

    public static class PrimitiveArrayNode extends ValueNode {
        @SuppressWarnings("unchecked")
        public static final Mapping mapping = mapping(PrimitiveArrayNode.class)
                .property("type", String.class, PrimitiveArrayNode::getType)
                .property("elements", List.class, PrimitiveArrayNode::getElements)
                .argsConstructor(props -> new PrimitiveArrayNode((String) props[0], (List<Object>) props[1]));

        private final String type;
        private final List<Object> elements;

        public PrimitiveArrayNode(String type, List<Object> elements) {
            this.type = type;
            if (!(type.endsWith("[]") && isPrimitive(getComponentType())))
                throw new IllegalArgumentException("Invalid primitive array type: " + type);

            this.elements = elements;
        }

        private String getType() {
            return type;
        }

        public String getComponentType() {
            return type.substring(0, type.length() - 2);
        }

        public List<Object> getElements() {
            return Collections.unmodifiableList(elements);
        }

        @Override
        public Mapping getMapping() {
            return mapping;
        }
    }

    private static boolean isPrimitive(String type) {
        return switch (type) {
            case "char", "byte", "short", "int", "long", "float", "double", "boolean" -> true;
            default -> false;
        };
    }
}
