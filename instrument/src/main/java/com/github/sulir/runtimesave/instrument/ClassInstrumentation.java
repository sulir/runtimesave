package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassInstrumentation {
    private final ClassNode classNode;
    private final LineAnalyzer lineAnalyzer = new LineAnalyzer();
    private final ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer();

    public ClassInstrumentation(ClassNode classNode) {
        this.classNode = classNode;
    }

    public void instrument(int everyNthLine) {
        for (MethodNode method : classNode.methods) {
            if (!excludeMethod(method)) {
                try {
                    LineCfg lineCfg = lineAnalyzer.analyze(method);
                    controlFlowAnalyzer.analyze(method, lineCfg);
                    new MethodInstrumentation(method, lineCfg, everyNthLine).instrument();
                } catch (LineNumberException ignored) { }
            }
        }
    }

    private boolean excludeMethod(MethodNode method) {
        return ((method.access & Opcodes.ACC_SYNTHETIC) != 0 && !method.name.startsWith("lambda$"))
                || method.name.equals("<clinit>")
                || method.instructions.size() == 0;
    }
}
