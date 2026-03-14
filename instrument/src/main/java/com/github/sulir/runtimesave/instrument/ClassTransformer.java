package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class ClassTransformer extends ClassVisitor {
    public static final String HITS_FIELD = "$runtimesaveHits";
    private static final int ASM_VERSION = Opcodes.ASM9;

    private final ControlFlowAnalyzer controlFlowAnalyzer = new ControlFlowAnalyzer();
    private boolean hasSource;
    private boolean hasInstrumentedMethods;
    private int lineId = 0;
    private String className;
    private MethodNode clInit;

    protected ClassTransformer(ClassWriter writer) {
        super(ASM_VERSION, writer);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if ((access & Opcodes.ACC_SYNTHETIC) != 0)
            throw ExcludedClassException.INSTANCE;
        className = name;
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

        if (name.equals("<clinit>")) {
            clInit = new MethodNode(ASM_VERSION, access, name, descriptor, signature, exceptions);
            return clInit;
        }

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

        if (Settings.HITS != -1)
            addHitsField();
        if (clInit != null)
            clInit.accept(cv);

        super.visitEnd();
    }

    private boolean methodExcluded(int access, String name) {
        return ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0
                || ((access & Opcodes.ACC_SYNTHETIC) != 0 && !name.startsWith("lambda$")));
    }

    private void instrumentMethod(MethodNode method) {
        AbstractInsnNode label = method.instructions.getFirst();
        if (!(label.getNext() instanceof LineNumberNode line && line.start == label))
            return;

        LineCfg lineCfg = new LineCfg(method.instructions, lineId);
        controlFlowAnalyzer.analyze(method, lineCfg);
        lineId = lineCfg.getNextLineId();
        new MethodInstrumentation(method, className, lineCfg).instrument();
    }

    private void addHitsField() {
        visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                HITS_FIELD, "[B", null, null);
        if (clInit == null) {
            clInit = new MethodNode(ASM_VERSION, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clInit.instructions.add(new InsnNode(Opcodes.RETURN));
        }
        instrumentClInit(clInit);
    }

    private void instrumentClInit(MethodNode clInit) {
        InsnList list = new InsnList();
        list.add(MethodInstrumentation.generatePush(lineId));
        list.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        list.add(new FieldInsnNode(Opcodes.PUTSTATIC, className, HITS_FIELD, "[B"));
        clInit.instructions.insert(list);
    }
}
