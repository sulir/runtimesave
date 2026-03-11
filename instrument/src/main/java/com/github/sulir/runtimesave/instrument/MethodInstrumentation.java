package com.github.sulir.runtimesave.instrument;

import com.github.sulir.runtimesave.runtime.Collector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MethodInstrumentation {
    private static final int INIT_LINE_ID = -1;
    private static final String COLLECTOR_CLASS = Type.getType(Collector.class).getInternalName();

    private final MethodNode method;
    private final LineCfg lineCfg;
    private final int everyNthLine;
    private final InsnList instructions;

    public MethodInstrumentation(MethodNode method, LineCfg lineCfg, int everyNthLine) {
        this.method = method;
        this.lineCfg = lineCfg;
        this.everyNthLine = everyNthLine;
        instructions = method.instructions;
    }

    public void instrument() {
        boolean linesTracked = instrumentMixedTargets();
        instrumentLineChangeTargets(linesTracked);
        printDebugInfo();
    }

    private boolean instrumentMixedTargets() {
        Collection<LabelNode> mixedTargets = lineCfg.getTargetsOfSameAndOtherLine();
        boolean trackingNeeded = false;

        for (LabelNode node : mixedTargets) {
            int lineId = lineCfg.findLineId(node);
            InsnList instrumentation = new InsnList();

            if (lineId % everyNthLine == 0) {
                instrumentation.add(generateCollectionIfLineChanged(lineId));
                trackingNeeded = true;
            }
            if (trackingNeeded && lineCfg.lineIsSourceOfMixedTarget(lineId))
                instrumentation.add(generateLineIdUpdate(lineId));
            instructions.insert(findInsertionPoint(node), instrumentation);
        }

        if (trackingNeeded) {
            instructions.insert(generateLineIdUpdate(INIT_LINE_ID));
            updateFramesForLineTracker();
        }

        return trackingNeeded;
    }

    private void instrumentLineChangeTargets(boolean trackLines) {
        for (LabelNode node : lineCfg.getTargetsOfOtherLineOnly()) {
            int lineId = lineCfg.findLineId(node);
            InsnList instrumentation = new InsnList();

            if (lineId % everyNthLine == 0)
                instrumentation.add(generateCollection(lineId));
            if (trackLines && lineCfg.lineIsSourceOfMixedTarget(lineId))
                instrumentation.add(generateLineIdUpdate(lineId));

            instructions.insert(findInsertionPoint(node), instrumentation);
        }
    }

    private void updateFramesForLineTracker() {
        for (AbstractInsnNode node : instructions) {
            if (node instanceof FrameNode frame) {
                int padding = method.maxLocals - countLocalSlots(frame);
                frame.local.addAll(Collections.nCopies(padding, Opcodes.TOP));
                frame.local.add(Opcodes.INTEGER);
            }
        }
    }

    private int countLocalSlots(FrameNode frame) {
        int slots = frame.local.size();
        for (Object local : frame.local) {
            if (local instanceof Integer code && (code.equals(Opcodes.LONG) || code.equals(Opcodes.DOUBLE)))
                slots++;
        }
        return slots;
    }

    private InsnList generateLineIdUpdate(int lineId) {
        InsnList result = new InsnList();
        result.add(generatePush(lineId));
        result.add(new VarInsnNode(Opcodes.ISTORE, method.maxLocals));
        return result;
    }

    private AbstractInsnNode generatePush(int constant) {
        if (constant >= -1 && constant <= 5)
            return new InsnNode(Opcodes.ICONST_0 + constant);

        if (constant >= Byte.MIN_VALUE && constant <= Byte.MAX_VALUE)
            return new IntInsnNode(Opcodes.BIPUSH, constant);

        if (constant >= Short.MIN_VALUE && constant <= Short.MAX_VALUE)
            return new IntInsnNode(Opcodes.SIPUSH, constant);

        return new LdcInsnNode(constant);
    }

    private AbstractInsnNode findInsertionPoint(AbstractInsnNode node) {
        while (node.getNext() != null && node.getNext().getOpcode() == -1)
            node = node.getNext();
        if (node.getNext() != null && node.getNext().getOpcode() == Opcodes.NEW)
            node = node.getNext();
        return node;
    }

    private InsnList generateCollectionIfLineChanged(int newLineId) {
        InsnList result = new InsnList();
        result.add(new VarInsnNode(Opcodes.ILOAD, method.maxLocals));
        result.add(generatePush(newLineId));
        if (everyNthLine != 1)
            result.add(generatePush(newLineId / everyNthLine));
        String descriptor = everyNthLine == 1 ? "(II)V" : "(III)V";
        result.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COLLECTOR_CLASS, "collectIfLineChanged", descriptor));
        return result;
    }

    private InsnList generateCollection(int lineId) {
        InsnList result = new InsnList();
        result.add(generatePush(lineId / everyNthLine));
        result.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COLLECTOR_CLASS, "collect", "(I)V"));
        return result;
    }

    private void printDebugInfo() {
        if (!InstrumentAgent.DEBUG)
            return;

        System.err.printf("--- %s%s ---\n", method.name, method.desc);
        Printer printer = new Textifier();
        TraceMethodVisitor tracer = new TraceMethodVisitor(printer);
        instructions.forEach(instruction -> instruction.accept(tracer));
        List<Object> lines = printer.getText();
        for (int i = 0; i < lines.size(); i++)
            System.err.printf("%d:%s", i, lines.get(i));
    }
}
