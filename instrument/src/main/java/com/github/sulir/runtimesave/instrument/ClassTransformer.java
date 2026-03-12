package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassTransformer extends ClassVisitor {
    private static final int ASM_VERSION = Opcodes.ASM9;

    private final ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer();
    private boolean hasSource;
    private boolean hasInstrumentedMethods;
    private int lineId = 0;

    protected ClassTransformer(ClassWriter writer) {
        super(ASM_VERSION, writer);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if ((access & Opcodes.ACC_SYNTHETIC) != 0)
            throw ExcludedClassException.INSTANCE;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        hasSource = true;
        super.visitSource(source, debug);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        if (!hasSource)
            throw ExcludedClassException.INSTANCE;

        MethodVisitor writer = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (excludeMethod(access, name))
            return writer;

        hasInstrumentedMethods = true;
        return new MethodNode(ASM_VERSION, access, name, descriptor, signature, exceptions) {
            @Override
            public void visitEnd() {
                instrumentMethod(this);
                accept(writer);
            }
        };
    }

    @Override
    public void visitEnd() {
        if (!hasSource || !hasInstrumentedMethods)
            throw ExcludedClassException.INSTANCE;
        super.visitEnd();
    }

    private boolean excludeMethod(int access, String name) {
        return ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0
                || ((access & Opcodes.ACC_SYNTHETIC) != 0 && !name.startsWith("lambda$"))
                || name.equals("<clinit>"));
    }

    private void instrumentMethod(MethodNode method) {
        AbstractInsnNode label = method.instructions.getFirst();
        if (!(label.getNext() instanceof LineNumberNode line && line.start == label))
            return;

        LineCfg lineCfg = controlFlowAnalyzer.analyze(method, lineId);
        lineId = lineCfg.getNextLineId();
        new MethodInstrumentation(method, lineCfg).instrument();
    }
}
