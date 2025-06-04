package com.github.sulir.runtimesave.db;

import com.sun.jdi.Location;

public class SourceLocation {
    private final String className;
    private final String method;
    private final int line;

    public static SourceLocation fromJDI(Location location) {
        String className = location.declaringType().name();
        String method = location.method().name() + location.method().signature();
        return new SourceLocation(className, method, location.lineNumber());
    }

    public static SourceLocation fromStackTrace(int level) {
        StackTraceElement frame = Thread.currentThread().getStackTrace()[level];
        return new SourceLocation(frame.getClassName(), frame.getMethodName(), frame.getLineNumber());
    }

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
