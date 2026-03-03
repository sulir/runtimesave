package com.github.sulir.runtimesave.instrument;

import com.github.sulir.runtimesave.runtime.Collector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.Collections;
import java.util.List;

public class MethodInstrumentation {
    private static final String COLLECTOR_CLASS = Type.getType(Collector.class).getInternalName();

    private final MethodNode method;
    private final String className;
    private final InsnList instructions;
    private boolean hasLineTracker = false;

    public MethodInstrumentation(MethodNode method, String className) {
        this.method = method;
        this.className = className;
        instructions = method.instructions;
    }

    public void instrument(int everyNthLine) {
        for (AbstractInsnNode node : instructions) {
            if (node instanceof LineNumberNode lineNode) {
                int lineNumber = lineNode.line;
                if (lineNumber % everyNthLine != 0)
                    continue;
                insertCallAfter(node);
            } else if (node instanceof FrameNode frame) {
                insertLineTrackerVar();
                updateFrameForLineTracker(frame);
            }
        }

        printDebugInfo();
    }

    private void insertLineTrackerVar() {
        if (hasLineTracker)
            return;
        hasLineTracker = true;

        InsnList init = new InsnList();
        init.add(new InsnNode(Opcodes.ICONST_0));
        init.add(new VarInsnNode(Opcodes.ISTORE, method.maxLocals));
        instructions.insert(init);
    }

    private void updateFrameForLineTracker(FrameNode frame) {
        int padding = method.maxLocals - countLocalSlots(frame);
        frame.local.addAll(Collections.nCopies(padding, Opcodes.TOP));
        frame.local.add(Opcodes.INTEGER);
    }

    private int countLocalSlots(FrameNode frame) {
        int slots = frame.local.size();
        for (Object local : frame.local) {
            if (local instanceof Integer code && (code.equals(Opcodes.LONG) || code.equals(Opcodes.DOUBLE)))
                slots++;
        }
        return slots;
    }

    private void insertCallAfter(AbstractInsnNode node) {
        if (node.getNext().getType() == AbstractInsnNode.FRAME)
            node = node.getNext();
        if (node.getNext().getOpcode() == Opcodes.NEW)
            node = node.getNext();

        MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKESTATIC, COLLECTOR_CLASS, "collect", "()V");
        instructions.insert(node, call);
    }

    private void printDebugInfo() {
        if (!InstrumentAgent.DEBUG)
            return;

        System.err.printf("--- %s %s%s ---\n", className, method.name, method.desc);
        Printer printer = new Textifier();
        TraceMethodVisitor tracer = new TraceMethodVisitor(printer);
        instructions.forEach(instruction -> instruction.accept(tracer));
        List<Object> lines = printer.getText();
        for (int i = 0; i < lines.size(); i++)
            System.err.printf("%d:%s", i, lines.get(i));
    }
}
