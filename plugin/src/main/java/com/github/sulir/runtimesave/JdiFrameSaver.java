package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.db.DBWriter;
import com.github.sulir.runtimesave.db.SourceLocation;
import com.sun.jdi.*;

import java.util.List;
import java.util.Map;

public class JdiFrameSaver {
    private static final int MAX_REFERENCE_LEVEL = -1;

    private final StackFrame frame;
    private final SourceLocation location;

    public JdiFrameSaver(StackFrame frame) {
        this.frame = frame;
        location = SourceLocation.fromJDI(frame.location());
    }

    public void saveThisAndLocals() {
        saveLocation();
        saveThisObject();
        saveLocalVariables();
        finish();
    }

    public void saveLocation() {
        DBWriter.getInstance().writeLocation(location);
    }

    public void saveThisObject() {
        if (frame.thisObject() != null)
            saveVariable("this", frame.thisObject());
    }

    public void saveLocalVariables() {
        try {
            List<LocalVariable> variables = frame.visibleVariables();
            Map<LocalVariable, Value> values = frame.getValues(variables);
            values.forEach((variable, value) -> saveVariable(variable.name(), value));
        } catch (AbsentInformationException ignored) { }
    }

    private void saveVariable(String name, Value value) {
        if (value instanceof PrimitiveValue primitive) {
            String type = value.type().name();
            DBWriter.getInstance().writePrimitiveVariable(location, name, type, toJavaPrimitive(primitive));
        } else if (value == null) {
            DBWriter.getInstance().writeNullVariable(location, name);
        } else if (value instanceof StringReference string) {
            DBWriter.getInstance().writeStringVariable(location, name, string.value());
        } else if (value instanceof ObjectReference object) {
            String type = object.referenceType().name();
            long jvmId = object.uniqueID();
            boolean created = DBWriter.getInstance().writeObjectVariable(location, name, type, jvmId);

            if (created) {
                if (value instanceof ArrayReference array)
                    saveElements(array, MAX_REFERENCE_LEVEL);
                else
                    saveFields(object, MAX_REFERENCE_LEVEL);
            }
        }
    }

    private void saveElements(ArrayReference array, int level) {
        long jvmId = array.uniqueID();

        if (level == 0)
            return;

        int index = 0;
        for (Value element : array.getValues()) {
            saveElement(jvmId, index, element, level - 1);
            index++;
        }
    }

    private void saveElement(long jvmId, int index, Value value, int level) {
        if (value instanceof PrimitiveValue primitive) {
            String type = value.type().name();
            DBWriter.getInstance().writePrimitiveElement(jvmId, index, type, toJavaPrimitive(primitive));
        } else if (value == null) {
            DBWriter.getInstance().writeNullElement(jvmId, index);
        } else if (value instanceof StringReference string) {
            DBWriter.getInstance().writeStringElement(jvmId, index, string.value());
        } else if (value instanceof ObjectReference object) {
            String type = object.referenceType().name();
            long childId = object.uniqueID();
            boolean created = DBWriter.getInstance().writeObjectElement(jvmId, index, type, childId);

            if (created) {
                if (value instanceof ArrayReference childArray)
                    saveElements(childArray, level);
                else
                    saveFields(object, level);
            }
        }
    }

    private void saveFields(ObjectReference object, int level) {
        long jvmId = object.uniqueID();

        if (level == 0)
            return;

        List<Field> fields = object.referenceType().visibleFields();
        fields.removeIf(Field::isStatic);
        Map<Field, Value> values = object.getValues(fields);
        values.forEach((field, value) -> saveField(jvmId, field.name(), value, level - 1));
    }

    private void saveField(long jvmId, String name, Value value, int level) {
        String type = (value == null) ? "null" : value.type().name();

        if (value instanceof PrimitiveValue primitive) {
            DBWriter.getInstance().writePrimitiveField(jvmId, name, type, toJavaPrimitive(primitive));
        } else if (value == null) {
            DBWriter.getInstance().writeNullField(jvmId, name);
        } else if (value instanceof StringReference string) {
            DBWriter.getInstance().writeStringField(jvmId, name, string.value());
        } else if (value instanceof ObjectReference object)  {
            long childID = object.uniqueID();
            boolean created = DBWriter.getInstance().writeObjectField(jvmId, name, type, childID);

            if (created) {
                if (object instanceof ArrayReference array)
                    saveElements(array, level);
                else
                    saveFields(object, level);
            }
        }
    }

    public void finish() {
        DBWriter.getInstance().deleteJvmIds();
    }

    private Object toJavaPrimitive(PrimitiveValue value) {
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
