package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassInstrumentation {
    private final ClassNode classNode;
    private final ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer();
    private int lineId = 0;

    public ClassInstrumentation(ClassNode classNode) {
        this.classNode = classNode;
    }

    public void instrument(int everyNthLine) {
        for (MethodNode method : classNode.methods) {
            if (!excludeMethod(method)) {
                LineCfg lineCfg = controlFlowAnalyzer.analyze(method, lineId);
                lineId = lineCfg.getNextLineId();
                new MethodInstrumentation(method, lineCfg, everyNthLine).instrument();
            }
        }
    }

    private boolean excludeMethod(MethodNode method) {
        AbstractInsnNode first = method.instructions.getFirst();

        return ((method.access & Opcodes.ACC_SYNTHETIC) != 0 && !method.name.startsWith("lambda$"))
                || method.name.equals("<clinit>")
                || method.instructions.size() == 0
                || !(first.getNext() instanceof LineNumberNode second && second.start == first);
    }
}
