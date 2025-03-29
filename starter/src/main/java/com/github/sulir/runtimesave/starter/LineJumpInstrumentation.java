package com.github.sulir.runtimesave.starter;

import org.objectweb.asm.tree.MethodNode;

public class LineJumpInstrumentation {
    private final MethodNode method;

    public LineJumpInstrumentation(MethodNode method) {
        this.method = method;
    }

    public void insert(int line) {
        
    }
}
