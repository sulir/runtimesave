package com.github.sulir.runtimesave;

import com.sun.jdi.*;

public class StatePersistence {
    private static final int MAX_REFERENCE_LEVEL = 5;

    private final StackFrame frame;
    private final SourceLocation location;

    public StatePersistence(StackFrame frame) {
        this.frame = frame;

        String className = frame.location().declaringType().name();
        String method = frame.location().method().name() + frame.location().method().signature();
        method = method.substring(0, method.lastIndexOf(')') + 1);
        location = new SourceLocation(className, method, frame.location().lineNumber());
    }

    public void saveThisAndLocals() {
        saveLocation();
        saveThisObject();
        saveLocalVariables();
        finish();
    }

    public void saveLocation() {
        Database.getInstance().writeLocation(location);
    }

    public void saveThisObject() {
        if (frame.thisObject() != null)
            saveVariable("this", frame.thisObject());
    }

    public void saveLocalVariables() {
        try {
            for (LocalVariable variable : frame.visibleVariables())
                saveVariable(variable.name(), frame.getValue(variable));
        } catch (AbsentInformationException ignored) { }
    }

    private void saveVariable(String name, Value value) {
        if (value instanceof PrimitiveValue) {
            String type = value.type().name();
            Database.getInstance().writePrimitiveVariable(location, name, type, value.toString());
        } else if (value == null) {
            Database.getInstance().writeObjectVariable(location, name, "null", -1);
        } else if (value instanceof ObjectReference object) {
            String type = object.referenceType().name();
            long objectID = object.uniqueID();
            boolean created = Database.getInstance().writeObjectVariable(location, name, type, objectID);

            if (created) {
                if (value instanceof StringReference string)
                    Database.getInstance().writeString(objectID, string.value());
                else
                    saveFields(object, MAX_REFERENCE_LEVEL);
            }
        }
    }

    private void saveFields(ObjectReference object, int level) {
        long objectID = object.uniqueID();

        if (level == 0)
            return;

        for (Field field : object.referenceType().visibleFields()) {
            if (field.isStatic())
                continue;

            saveField(objectID, field.name(), object.getValue(field), level - 1);
        }
    }

    private void saveField(long objectID, String name, Value value, int level) {
        String type = (value == null) ? "null" : value.type().name();

        if (value instanceof PrimitiveValue) {
            Database.getInstance().writePrimitiveField(objectID, name, type, value.toString());
        } else if (value == null) {
            Database.getInstance().writeObjectField(objectID, name, type, -1);
        } else if (value instanceof ObjectReference object)  {
            long childID = object.uniqueID();
            boolean created = Database.getInstance().writeObjectField(objectID, name, type, childID);

            if (created) {
                if (value instanceof StringReference string)
                    Database.getInstance().writeString(childID, string.value());
                else
                    saveFields(object, level);
            }
        }
    }

    public void finish() {
        Database.getInstance().deleteObjectIDs();
    }
}
