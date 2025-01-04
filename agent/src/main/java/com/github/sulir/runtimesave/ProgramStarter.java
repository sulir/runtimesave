package com.github.sulir.runtimesave;

import sun.misc.Unsafe;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProgramStarter {
    public static void main(String[] args) throws Throwable {
        String className = args[0];
        String methodName = args[1];
        String paramsDescriptor = args[2];
        new ProgramStarter().start(className, methodName, paramsDescriptor);
    }

    public void start(String className, String methodName, String paramsDescriptor) throws Throwable  {
        for (Method method : Class.forName(className).getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                String methodParams = getParamsDescriptor(method);
                if (methodParams.equals(paramsDescriptor)) {
                    executeMethod(method);
                    break;
                }
            }
        }
    }

    private void executeMethod(Method method) throws Throwable {
        @SuppressWarnings("deprecation")
        boolean wasAccessible = method.isAccessible();
        if (!wasAccessible) {
            method.setAccessible(true);
        }

        Object thisObject = createThisObject(method);

        String className = method.getDeclaringClass().getName();
        String methodSignature = method.getName() + getParamsDescriptor(method);

        List<Object> params = Arrays.stream(method.getParameters())
                .map(p -> readVariable(className, methodSignature, p.getName(), p.getType()))
                .toList();

        try {
            method.invoke(thisObject, params.toArray());
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        method.setAccessible(wasAccessible);
    }

    private Object createThisObject(Method method) throws ReflectiveOperationException {
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
        } else {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            Object thisObject = ((Unsafe) unsafe.get(null)).allocateInstance(method.getDeclaringClass());

            for (Field field : thisObject.getClass().getDeclaredFields()) {
                // inherited are missing
            }

            return thisObject;
        }
    }

    private Object readVariable(String className, String method, String variableName, Class<?> variableType) {
        String primitiveValue;
        if (variableType.isPrimitive())
            primitiveValue = Database.getInstance().readPrimitiveVariable(className, method, variableName);
        else
            primitiveValue = "";

        return switch(variableType.getName()) {
            case "char" -> primitiveValue.charAt(0);
            case "byte", "short", "int" -> Integer.valueOf(primitiveValue);
            case "long" -> Long.valueOf(primitiveValue);
            case "float" -> Float.valueOf(primitiveValue);
            case "double" -> Double.valueOf(primitiveValue);
            case "boolean" -> Boolean.valueOf(primitiveValue);
            case "java.lang.String" -> Database.getInstance().readStringVariable(className, method, variableName);
            default -> null;
        };
    }

    private String getParamsDescriptor(Method method) {
        return "(" + Arrays.stream(method.getParameters())
                .map(Parameter::getType)
                .map(Class::descriptorString)
                .collect(Collectors.joining()) + ")";
    }
}
