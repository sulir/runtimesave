package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;

import java.util.*;

public class LineCfg {
    private final InsnList instructions;
    private int lineId;
    private final Map<AbstractInsnNode, Integer> instructionToLine;
    private final Map<Integer, Integer> lineToId = new HashMap<>();
    private final Set<AbstractInsnNode> targetOfSameLine = new HashSet<>();
    private final Set<AbstractInsnNode> targetOfOtherLine = new HashSet<>();
    private final Set<FrameNode> frames = new HashSet<>();

    public LineCfg(InsnList instructions, int startLineId) {
        this.instructions = instructions;
        lineId = startLineId;
        instructionToLine = new HashMap<>(instructions.size());
        targetOfOtherLine.add(instructions.getFirst());
    }

    public void setLineNumber(AbstractInsnNode instruction, int lineNumber) {
        instructionToLine.put(instruction, lineNumber);
        lineToId.computeIfAbsent(lineNumber, x -> lineId++);
    }

    public void addEdge(int fromIndex, int toIndex) {
        AbstractInsnNode from = instructions.get(fromIndex);
        AbstractInsnNode to = instructions.get(toIndex);
        Integer fromLine = instructionToLine.get(from);
        Integer toLine = instructionToLine.get(to);
        if (fromLine == null || toLine == null)
            throw new IllegalArgumentException("Instruction lacks line number");

        if (fromLine.equals(toLine))
            targetOfSameLine.add(to);
        else
            targetOfOtherLine.add(to);

        if (to instanceof FrameNode frame)
            frames.add(frame);
    }

    public Collection<AbstractInsnNode> getTargetsOfOtherLineOnly() {
        Set<AbstractInsnNode> result = new HashSet<>(targetOfOtherLine);
        result.removeAll(targetOfSameLine);
        return result;
    }

    public Collection<AbstractInsnNode> getTargetsOfSameAndOtherLine() {
        Set<AbstractInsnNode> result = new HashSet<>(targetOfOtherLine);
        result.retainAll(targetOfSameLine);
        return result;
    }

    public Collection<FrameNode> getFrames() {
        return frames;
    }

    public int getLine(AbstractInsnNode instruction) {
        return instructionToLine.get(instruction);
    }

    public int getLineId(AbstractInsnNode instruction) {
        return lineToId.get(getLine(instruction));
    }

    public int getNextLineId() {
        return lineId;
    }
}
