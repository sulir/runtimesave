package com.github.sulir.runtimesave.db;

import com.sun.jdi.Location;

public class SourceLocation {
    final String className;
    final String method;
    final int line;

    public static SourceLocation fromJDI(Location location) {
        String className = location.declaringType().name();
        String method = location.method().name() + location.method().signature();
        method = method.substring(0, method.lastIndexOf(')') + 1);
        return new SourceLocation(className, method, location.lineNumber());
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
