package io.github.sulir.runtimesave.nodes;

import io.github.sulir.runtimesave.graph.Mapping;
import io.github.sulir.runtimesave.graph.ValueNode;

import java.util.Collections;
import java.util.List;

public class PrimitiveArrayNode extends ValueNode {
    @SuppressWarnings("unchecked")
    public static final Mapping mapping = mapping(PrimitiveArrayNode.class)
            .property("type", String.class, PrimitiveArrayNode::getType)
            .property("elements", List.class, PrimitiveArrayNode::getElements)
            .argsConstructor(props -> new PrimitiveArrayNode((String) props[0], (List<Object>) props[1]));

    private String type;
    private List<Object> elements;

    public PrimitiveArrayNode(String type, List<Object> elements) {
        checkType(type);
        this.type = type;
        this.elements = elements;
    }

    public PrimitiveArrayNode() { }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        checkType(type);
        checkModification();
        this.type = type;
    }

    public String getComponentType() {
        return getComponentType(type);
    }

    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void setElements(List<Object> elements) {
        checkModification();
        this.elements = elements;
    }

    public int getLength() {
        return elements.size();
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }

    public static boolean isPrimitive(String type) {
        return switch (type) {
            case "char", "byte", "short", "int", "long", "float", "double", "boolean" -> true;
            default -> false;
        };
    }

    private static void checkType(String type) {
        if (!type.endsWith("[]") || !isPrimitive(getComponentType(type)))
            throw new IllegalArgumentException("Invalid primitive array type: " + type);
    }

    private static String getComponentType(String type) {
        return type.substring(0, type.length() - 2);
    }
}
