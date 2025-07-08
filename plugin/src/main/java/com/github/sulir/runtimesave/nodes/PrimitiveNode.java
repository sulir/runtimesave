package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;

public class PrimitiveNode extends ValueNode {
    private static final Mapping mapping = mapping(PrimitiveNode.class)
            .property("value", Object.class, PrimitiveNode::getValue)
            .property("type", String.class, PrimitiveNode::getType)
            .argsConstructor(props -> new PrimitiveNode(props[0], (String) props[1]));

    private final Object value;
    private final String type;

    public PrimitiveNode(Object value, String type) {
        this.value = switch (type) {
            case "char" -> value instanceof String string ? string.charAt(0) : (Character) value;
            case "byte" -> ((Number) value).byteValue();
            case "short" -> ((Number) value).shortValue();
            case "int" -> ((Number) value).intValue();
            case "long" -> ((Number) value).longValue();
            case "float" -> ((Number) value).floatValue();
            case "double" -> ((Number) value).doubleValue();
            case "boolean" -> (Boolean) value;
            default -> throw new IllegalArgumentException("Not primitive: " + type);
        };
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }
}
