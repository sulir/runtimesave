package com.github.sulir.runtimesave.instrument;

import com.github.sulir.runtimesave.runtime.Collector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class MethodInstrumentation {
    private static final String COLLECTOR_CLASS = Type.getType(Collector.class).getInternalName();

    private final MethodNode method;
    private final InsnList instructions;

    public MethodInstrumentation(MethodNode method) {
        this.method = method;
        this.instructions = method.instructions;
    }

    public void instrument(int everyNthLine) {
        for (AbstractInsnNode node = instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof LineNumberNode lineNode) {
                int lineNumber = lineNode.line;
                if (lineNumber % everyNthLine != 0)
                    continue;

                if (node.getNext() instanceof FrameNode)
                    node = node.getNext();
                if (node.getNext().getOpcode() == Opcodes.NEW)
                    node = node.getNext();
                insertCallAfter(node);
            }
        }

        printDebugInfo();
    }

    private void insertCallAfter(AbstractInsnNode node) {
        MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKESTATIC, COLLECTOR_CLASS, "collect", "()V");
        instructions.insert(node, call);
    }

    private void printDebugInfo() {
        if (!InstrumentAgent.DEBUG)
            return;

        System.err.println("--- " + method.name + method.desc + " ---");
        Printer printer = new Textifier();
        TraceMethodVisitor tracer = new TraceMethodVisitor(printer);
        instructions.forEach(instruction -> instruction.accept(tracer));
        printer.getText().forEach(System.err::print);
    }
}
