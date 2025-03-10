package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.db.SourceLocation;
import com.github.sulir.runtimesave.graph.*;
import com.sun.jdi.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdiFrameLoader {
    private static final String UNSAFE_HELPER = "com.github.sulir.runtimesave.starter.UnsafeHelper";
    public static final String PUT_VALUE_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/String;%s)V";

    private StackFrame frame;
    private final VirtualMachine vm;
    private final SourceLocation location;
    private final Map<ReferenceNode, ObjectReference> visited = new HashMap<>();

    public JdiFrameLoader(StackFrame frame) {
        this.frame = frame;
        vm = frame.virtualMachine();
        location = SourceLocation.fromJDI(frame.location());
    }

    public void loadThisAndLocals() {
        loadThisObjectFields();
        loadLocalVariables();
    }

    public void loadThisObjectFields() {
        ObjectReference thisObject = frame.thisObject();
        if (thisObject != null) {
            GraphNode node = GraphNode.findVariable(location.getClassName(), location.getMethod(), "this");
            assignFields(thisObject, (ObjectNode) node);
        }
    }

    public void loadLocalVariables() {
        try {
            for (LocalVariable variable : frame.visibleVariables()) {
                GraphNode node = GraphNode.findVariable(location.getClassName(), location.getMethod(), variable.name());
                assignVariable(variable, node);
            }
        } catch (AbsentInformationException ignored) { }
    }

    private interface ValueAssigner {
        void setValue(Value value) throws InvalidTypeException, ClassNotLoadedException;
    }

    private void assignVariable(LocalVariable variable, GraphNode node) {
        assignValue((value) -> frame.setValue(variable, value), node);
    }

    private void assignField(ObjectReference object, Field field, GraphNode node) {
        assignValue((value) -> {
            if (!field.isFinal())
                object.setValue(field, value);
            else
                setFinalFieldValue(object, field, value);
        }, node);
    }

    private void setFinalFieldValue(ObjectReference object, Field field, Value value) {
        ClassType helper = (ClassType) vm.classesByName(UNSAFE_HELPER).get(0);

        String valueSignature = value instanceof PrimitiveValue ? value.type().signature() : "Ljava/lang/Object;";
        String argsSignature = String.format(PUT_VALUE_SIGNATURE, valueSignature);
        Method method = helper.concreteMethodByName("putValue", argsSignature);
        
        List<Value> args = List.of(object, vm.mirrorOf(field.name()), value);
        try {
            ThreadReference thread = frame.thread();
            helper.invokeMethod(thread, method, args, ClassType.INVOKE_SINGLE_THREADED);
            frame = thread.frame(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                    assignElements(object, arrayNode);
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

    private ObjectReference allocateArray(ArrayNode arrayNode) {
        return null;
    }

    private void assignElements(ObjectReference arrayRef, ArrayNode arrayNode) {

    }

    private ObjectReference allocateObject(ObjectNode node) {
        String className = node.getType();
        ClassType creator = (ClassType) vm.classesByName(UNSAFE_HELPER).get(0);
        Method method = creator.methodsByName("allocateInstance").get(0);
        List<Value> args = List.of(frame.virtualMachine().mirrorOf(className));
        try {
            ThreadReference thread = frame.thread();
            Value result = creator.invokeMethod(thread, method, args, ClassType.INVOKE_SINGLE_THREADED);
            frame = thread.frame(0);
            return (ObjectReference) result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assignFields(ObjectReference object, ObjectNode objectNode) {
        for (Field field : object.referenceType().visibleFields()) {
            if (field.isStatic())
                continue;

            assignField(object, field, objectNode.getField(field.name()));
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
