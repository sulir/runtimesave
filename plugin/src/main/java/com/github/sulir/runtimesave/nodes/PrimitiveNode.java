package com.github.sulir.runtimesave.nodes;

public class PrimitiveNode extends ValueNode {
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
}
