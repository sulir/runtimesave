package com.github.sulir.runtimesave.jdi;

import com.github.sulir.runtimesave.MismatchException;
import com.github.sulir.runtimesave.nodes.*;
import com.sun.jdi.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdiWriter {
    private static final String UNSAFE_HELPER = "com.github.sulir.runtimesave.starter.UnsafeHelper";

    private StackFrame frame;
    private final VirtualMachine vm;
    private final Map<ReferenceNode, ObjectReference> visited = new HashMap<>();

    public JdiWriter(StackFrame frame) {
        this.frame = frame;
        vm = frame.virtualMachine();
    }

    public void writeFrame(FrameNode node) throws MismatchException {
        writeThisObjectFields(node);
        writeLocalVariables(node);
    }

    public void writeVariable(LocalVariable variable, GraphNode node) {
        assignValue((value) -> frame.setValue(variable, value), node);
    }

    private void writeThisObjectFields(FrameNode frameNode) throws MismatchException {
        ObjectReference thisObject = frame.thisObject();
        if (thisObject != null) {
            GraphNode thisNode = frameNode.getVariable("this");
            if (thisNode instanceof ObjectNode objectNode)
                assignFields(thisObject, objectNode);
            else
                throw new MismatchException("Object 'this' not found or invalid");
        }
    }

    private void writeLocalVariables(FrameNode frameNode) throws MismatchException {
        try {
            for (LocalVariable variable : frame.visibleVariables()) {
                GraphNode node = frameNode.getVariable(variable.name());
                if (node != null)
                    writeVariable(variable, node);
                else
                    throw new MismatchException("Variable " + variable.name() + " not found");
            }
        } catch (AbsentInformationException ignored) { }
    }

    private interface ValueAssigner {
        void setValue(Value value) throws InvalidTypeException, ClassNotLoadedException;
    }

    private void assignField(ObjectReference object, Field field, GraphNode node) {
        assignValue((value) -> {
            if (!field.isFinal())
                object.setValue(field, value);
            else
                setFinalFieldValue(object, field, value);
        }, node);
    }

    private void assignElement(ArrayReference array, int index, GraphNode node) {
        assignValue((value) -> array.setValue(index, value), node);
    }

    private void assignValue(ValueAssigner assigner, GraphNode node) {
        try {
            if (node instanceof PrimitiveNode primitiveNode) {
                assigner.setValue(toJdiPrimitive(primitiveNode.getValue()));
            } else if (node instanceof NullNode) {
                assigner.setValue(null);
            } else if (node instanceof StringNode stringNode) {
                assigner.setValue(vm.mirrorOf(stringNode.getValue()));
            } else if (node instanceof ReferenceNode referenceNode) {
                ObjectReference existing = visited.get(referenceNode);
                if (existing != null) {
                    assigner.setValue(existing);
                    return;
                }

                ObjectReference object = null;
                if (referenceNode instanceof ArrayNode arrayNode) {
                    object = allocateArray(arrayNode);
                    visited.put(arrayNode, object);
                    assignElements((ArrayReference) object, arrayNode);
                } else if (referenceNode instanceof ObjectNode objectNode) {
                    object = allocateObject(objectNode);
                    visited.put(objectNode, object);
                    assignFields(object, objectNode);
                }
                assigner.setValue(object);
            }
        } catch (InvalidTypeException | ClassNotLoadedException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectReference allocateArray(ArrayNode node) {
        Value result = invokeHelperMethod("allocateArray", "(Ljava/lang/String;I)Ljava/lang/Object;",
                List.of(vm.mirrorOf(node.getType()), vm.mirrorOf(node.getSize())));
        return (ObjectReference) result;
    }

    private void assignElements(ArrayReference array, ArrayNode node) {
        int size = node.getSize();

        for (int i = 0; i < size; i++)
            assignElement(array, i, node.getElement(i));
    }

    private ObjectReference allocateObject(ObjectNode node) {
        Value result = invokeHelperMethod("allocateInstance", "(Ljava/lang/String;)Ljava/lang/Object;",
                List.of(vm.mirrorOf(node.getType())));
        return (ObjectReference) result;
    }

    private void assignFields(ObjectReference object, ObjectNode objectNode) {
        for (Field field : object.referenceType().visibleFields()) {
            if (field.isStatic())
                continue;

            assignField(object, field, objectNode.getField(field.name()));
        }
    }

    private void setFinalFieldValue(ObjectReference object, Field field, Value value) {
        String valueSignature = value instanceof PrimitiveValue ? value.type().signature() : "Ljava/lang/Object;";
        String argsSignature = String.format("(Ljava/lang/Object;Ljava/lang/String;%s)V", valueSignature);
        invokeHelperMethod("putValue", argsSignature, List.of(object, vm.mirrorOf(field.name()), value));
    }

    private Value invokeHelperMethod(String methodName, String signature, List<Value> args) {
        ClassType helper = (ClassType) vm.classesByName(UNSAFE_HELPER).get(0);
        Method method = helper.concreteMethodByName(methodName, signature);
        try {
            ThreadReference thread = frame.thread();
            Value result = helper.invokeMethod(thread, method, args, ClassType.INVOKE_SINGLE_THREADED);
            frame = thread.frame(0);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PrimitiveValue toJdiPrimitive(Object value) {
        return switch(value.getClass().getSimpleName()) {
            case "Character" -> vm.mirrorOf((char) value);
            case "Byte" -> vm.mirrorOf((byte) value);
            case "Short" -> vm.mirrorOf((short) value);
            case "Integer" -> vm.mirrorOf((int) value);
            case "Long" -> vm.mirrorOf((long) value);
            case "Float" -> vm.mirrorOf((float) value);
            case "Double" -> vm.mirrorOf((double) value);
            case "Boolean" -> vm.mirrorOf((boolean) value);
            default -> throw new IllegalArgumentException("Unknown primitive type: " + value.getClass());
        };
    }
}
