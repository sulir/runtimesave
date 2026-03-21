package com.github.sulir.runtimesave.rt;

public record JvmSourceLocation(String className, String method, int line) {
    @SuppressWarnings("unused")
    public static JvmSourceLocation fromJvmTi(String classSig, String methodName, String methodSig, int line) {
        String className = classSig.substring(1, classSig.length() - 1).replace('/', '.');
        return new JvmSourceLocation(className, methodName + methodSig, line);
    }

    public String toString() {
        return className + "." + method + ":" + line;
    }
}
