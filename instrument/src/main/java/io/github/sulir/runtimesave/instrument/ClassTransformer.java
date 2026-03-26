package io.github.sulir.runtimesave.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.concurrent.atomic.AtomicInteger;

public class ClassTransformer extends ClassVisitor {
    private static final int ASM_VERSION = Opcodes.ASM9;

    private final ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer();
    private final AtomicInteger lineId;
    private boolean hasSource;
    private boolean hasInstrumentedMethods;

    protected ClassTransformer(ClassWriter writer, AtomicInteger lineId) {
        super(ASM_VERSION, writer);
        this.lineId = lineId;
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
        if (methodExcluded(access, name))
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

    private boolean methodExcluded(int access, String name) {
        return ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0
                || ((access & Opcodes.ACC_SYNTHETIC) != 0 && !name.startsWith("lambda$"))
                || name.equals("<clinit>"));
    }

    private void instrumentMethod(MethodNode method) {
        AbstractInsnNode label = method.instructions.getFirst();
        if (!(label.getNext() instanceof LineNumberNode line && line.start == label))
            return;

        LineCfg lineCfg = new LineCfg(method.instructions, lineId);
        controlFlowAnalyzer.analyze(method, lineCfg);
        new MethodInstrumentation(method, lineCfg).instrument();
    }
}
