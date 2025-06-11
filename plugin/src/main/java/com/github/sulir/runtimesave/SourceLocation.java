package com.github.sulir.runtimesave;

import com.sun.jdi.Location;

public record SourceLocation(String className, String method, int line) {
    public static SourceLocation fromJDI(Location location) {
        String className = location.declaringType().name();
        String method = location.method().name() + location.method().signature();
        return new SourceLocation(className, method, location.lineNumber());
    }
}
