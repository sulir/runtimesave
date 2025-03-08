package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.db.SourceLocation;
import com.sun.jdi.StackFrame;

public class JdiLoader {
    private final StackFrame frame;

    public JdiLoader(StackFrame frame) {
        this.frame = frame;
    }

    public void loadThisAndLocals() {
        SourceLocation sourceLocation = SourceLocation.fromJDI(frame.location());
    }
}
