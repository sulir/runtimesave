package com.github.sulir.runtimesave.starter;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StarterMain {
    public static void main(String[] args) throws Throwable {
        String className = args[0];
        String methodName = args[1];
        String paramsDescriptor = args[2];
        new StarterMain().start(className, methodName, paramsDescriptor);
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

    private String getParamsDescriptor(Method method) {
        return "(" + Arrays.stream(method.getParameters())
                .map(Parameter::getType)
                .map(Class::descriptorString)
                .collect(Collectors.joining()) + ")";
    }

    private void executeMethod(Method method) throws Throwable {
        @SuppressWarnings("deprecation")
        boolean wasAccessible = method.isAccessible();
        if (!wasAccessible) {
            method.setAccessible(true);
        }

        Object thisObject = createThisObject(method);
        List<Object> params = Arrays.stream(method.getParameters())
                .map(p -> createDefaultValue(p.getType()))
                .toList();

        try {
            method.invoke(thisObject, params.toArray());
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        method.setAccessible(wasAccessible);
    }

    private Object createThisObject(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return null;
        } else {
            return UnsafeHelper.allocateInstance(method.getDeclaringClass().getName());
        }
    }

    private Object createDefaultValue(Class<?> type) {
        return Array.get(Array.newInstance(type, 1), 0);
    }
}
