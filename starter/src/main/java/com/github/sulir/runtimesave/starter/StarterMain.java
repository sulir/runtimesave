package com.github.sulir.runtimesave.starter;

import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.JavaObjectGraph;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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
                .map(p -> readVariable(className, methodSignature, p.getName()))
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
            GraphNode graphNode = GraphNode.findVariable(method.getDeclaringClass().getName(),
                    method.getName() + getParamsDescriptor(method), "this");
            return new JavaObjectGraph(graphNode).create();
        }
    }

    private Object readVariable(String className, String method, String variableName) {
        GraphNode graphNode = GraphNode.findVariable(className, method, variableName);
        return new JavaObjectGraph(graphNode).create();
    }

    private String getParamsDescriptor(Method method) {
        return "(" + Arrays.stream(method.getParameters())
                .map(Parameter::getType)
                .map(Class::descriptorString)
                .collect(Collectors.joining()) + ")";
    }
}
