package com.github.sulir.runtimesave;

public class SourceLocation {
    final String className;
    final String method;
    final int line;

    public SourceLocation(String className, String method, int line) {
        this.className = className;
        this.method = method;
        this.line = line;
    }

    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public int getLine() {
        return line;
    }
}
