package com.github.sulir.runtimesave.jdi;

import com.github.sulir.runtimesave.nodes.*;
import com.sun.jdi.*;

import java.util.List;
import java.util.Map;

public class JdiReader {
    private final StackFrame frame;
    private final Map<Long, GraphNode> created = new java.util.HashMap<>();

    public JdiReader(StackFrame frame) {
        this.frame = frame;
    }

    public FrameNode readFrame() {
        FrameNode frameNode = new FrameNode();
        addThisObject(frameNode);
        addLocalVariables(frameNode);
        return frameNode;
    }

    private void addThisObject(FrameNode frameNode) {
        if (frame.thisObject() != null)
            frameNode.setVariable("this", createNode(frame.thisObject()));
    }

    private void addLocalVariables(FrameNode frameNode) {
        try {
            List<LocalVariable> variables = frame.visibleVariables();
            Map<LocalVariable, Value> values = frame.getValues(variables);
            values.forEach((variable, value) -> frameNode.setVariable(variable.name(), createNode(value)));
        } catch (AbsentInformationException ignored) { }
    }

    private GraphNode createNode(Value value) {
        if (value instanceof PrimitiveValue primitive) {
            return new PrimitiveNode(toBoxed(primitive), primitive.type().name());
        } else if (value == null) {
            return new NullNode();
        } else if (value instanceof StringReference string) {
            return new StringNode(string.value());
        } else if (value instanceof ObjectReference object) {
            GraphNode existing = created.get(object.uniqueID());
            if (existing != null)
                return existing;

            String type = object.referenceType().name();
            GraphNode node = value instanceof ArrayReference ? new ArrayNode(type) : new ObjectNode(type);
            created.put(object.uniqueID(), node);

            if (value instanceof ArrayReference array)
                addElements((ArrayNode) node, array);
            else
                saveFields((ObjectNode) node, object);

            return node;
        } else {
            throw new IllegalArgumentException("Unknown value type: " + value.type().name());
        }
    }

    private void addElements(ArrayNode arrayNode, ArrayReference array) {
        for (Value element : array.getValues())
            arrayNode.addElement(createNode(element));
    }

    private void saveFields(ObjectNode objectNode, ObjectReference object) {
        List<Field> fields = object.referenceType().visibleFields();
        fields.removeIf(Field::isStatic);
        Map<Field, Value> values = object.getValues(fields);
        values.forEach((field, value) -> objectNode.setField(field.name(), createNode(value)));
    }

    private Object toBoxed(PrimitiveValue value) {
        return switch (value.type().name()) {
            case "char" -> value.charValue();
            case "byte" -> value.byteValue();
            case "short" -> value.shortValue();
            case "int" -> value.intValue();
            case "long" -> value.longValue();
            case "float" -> value.floatValue();
            case "double" -> value.doubleValue();
            case "boolean" -> value.booleanValue();
            default -> throw new IllegalArgumentException("Unknown primitive type: " + value.type().name());
        };
    }
}
