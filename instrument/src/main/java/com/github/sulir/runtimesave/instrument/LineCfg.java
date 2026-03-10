package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.*;

public class LineCfg {
    private enum TargetKind { FROM_SAME_LINE, FROM_OTHER_LINE, MIXED }

    private int lineId;
    private final Map<AbstractInsnNode, Integer> instructionToLine;
    private final Map<Integer, Integer> lineToId = new HashMap<>();
    private final Map<Integer, Set<LabelNode>> lineIdToTargets = new HashMap<>();
    private final Map<LabelNode, TargetKind> targetKinds = new IdentityHashMap<>();
    private final Set<FrameNode> frames = new HashSet<>();

    public LineCfg(InsnList instructions, int startLineId) {
        lineId = startLineId;
        instructionToLine = new IdentityHashMap<>(instructions.size());
        targetKinds.put((LabelNode) instructions.getFirst(), TargetKind.FROM_OTHER_LINE);
    }

    public void setLineNumber(AbstractInsnNode instruction, int lineNumber) {
        instructionToLine.put(instruction, lineNumber);
        lineToId.computeIfAbsent(lineNumber, x -> lineId++);
    }

    public void addEdge(AbstractInsnNode from, AbstractInsnNode to) {
        Integer fromLine = instructionToLine.get(from);
        Integer toLine = instructionToLine.get(to);
        if (fromLine == null || toLine == null)
            throw new IllegalArgumentException("Instruction lacks line number");

        if (to instanceof LabelNode target) {
            int lineId = lineToId.get(fromLine);
            lineIdToTargets.computeIfAbsent(lineId, x -> new HashSet<>()).add(target);

            TargetKind kind = targetKinds.get(target);
            boolean same = kind == TargetKind.FROM_SAME_LINE || kind == TargetKind.MIXED || fromLine.equals(toLine);
            boolean other = kind == TargetKind.FROM_OTHER_LINE || kind == TargetKind.MIXED || !fromLine.equals(toLine);

            if (same && other)
                targetKinds.put(target, TargetKind.MIXED);
            else if (same)
                targetKinds.put(target, TargetKind.FROM_SAME_LINE);
            else
                targetKinds.put(target, TargetKind.FROM_OTHER_LINE);
        } else if (to instanceof FrameNode frame) {
            frames.add(frame);
        }
    }

    public Collection<LabelNode> getTargetsOfOtherLineOnly() {
        return targetKinds.entrySet().stream()
                .filter(e -> e.getValue() == TargetKind.FROM_OTHER_LINE)
                .map(Map.Entry::getKey)
                .toList();
    }

    public Collection<LabelNode> getTargetsOfSameAndOtherLine() {
        return targetKinds.entrySet().stream()
                .filter(e -> e.getValue() == TargetKind.MIXED)
                .map(Map.Entry::getKey)
                .toList();
    }

    public boolean lineIsSourceOfMixedTarget(int lineId) {
        for (LabelNode target : lineIdToTargets.getOrDefault(lineId, Collections.emptySet())) {
            if (targetKinds.get(target) == TargetKind.MIXED)
                return true;
        }
        return false;
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
