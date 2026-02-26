package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassInstrumentation {
    private final ClassNode classNode;

    public ClassInstrumentation(ClassNode classNode) {
        this.classNode = classNode;
    }

    public void instrument(int everyNthLine, int firstTExecutions) {
        for (MethodNode method : classNode.methods)
            if (!excludeMethod(method))
                new MethodInstrumentation(method).instrument(everyNthLine);
    }

    private boolean excludeMethod(MethodNode method) {
        return method.instructions.size() == 0
                || (method.access & Opcodes.ACC_SYNTHETIC) != 0
                || method.name.equals("<clinit>");
    }
}
