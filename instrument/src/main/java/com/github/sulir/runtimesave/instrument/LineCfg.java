package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.util.*;

public class LineCfg {
    private enum TargetKind { FROM_SAME_LINE, FROM_OTHER_LINE, MIXED }

    private int lineId;
    private final Map<Integer, Integer> lineToId = new HashMap<>();
    private final Map<Integer, Set<LabelNode>> lineIdToTargets = new HashMap<>();
    private final Map<LabelNode, TargetKind> targetKinds = new IdentityHashMap<>();

    public LineCfg(InsnList instructions, int startLineId) {
        lineId = startLineId;
        targetKinds.put((LabelNode) instructions.getFirst(), TargetKind.FROM_OTHER_LINE);
    }

    public void addEdge(AbstractInsnNode from, AbstractInsnNode to) {
        saveNewLineId(from);
        if (!(to instanceof LabelNode target))
            return;

        int fromLine = findLine(from);
        int toLine = findLine(to);
        int fromLineId = lineToId.computeIfAbsent(fromLine, x -> lineId++);
        lineIdToTargets.computeIfAbsent(fromLineId, x -> new HashSet<>()).add(target);

        TargetKind kind = targetKinds.get(target);
        boolean same = kind == TargetKind.FROM_SAME_LINE || kind == TargetKind.MIXED || fromLine == toLine;
        boolean other = kind == TargetKind.FROM_OTHER_LINE || kind == TargetKind.MIXED || fromLine != toLine;

        if (same && other)
            targetKinds.put(target, TargetKind.MIXED);
        else if (same)
            targetKinds.put(target, TargetKind.FROM_SAME_LINE);
        else
            targetKinds.put(target, TargetKind.FROM_OTHER_LINE);
    }

    private void saveNewLineId(AbstractInsnNode instruction) {
        if (instruction.getNext() instanceof LineNumberNode)
            lineToId.computeIfAbsent(findLine(instruction), x -> lineId++);
    }

    public Collection<LabelNode> getTargetsOfOtherLineOnly() {
        ArrayList<LabelNode> result = new ArrayList<>();
        for (Map.Entry<LabelNode, TargetKind> entry : targetKinds.entrySet())
            if (entry.getValue() == TargetKind.FROM_OTHER_LINE)
                result.add(entry.getKey());
        return result;
    }

    public Collection<LabelNode> getTargetsOfSameAndOtherLine() {
        ArrayList<LabelNode> result = new ArrayList<>();
        for (Map.Entry<LabelNode, TargetKind> entry : targetKinds.entrySet())
            if (entry.getValue() == TargetKind.MIXED)
                result.add(entry.getKey());
        return result;
    }

    public boolean lineIsSourceOfMixedTarget(int lineId) {
        for (LabelNode target : lineIdToTargets.getOrDefault(lineId, Collections.emptySet())) {
            if (targetKinds.get(target) == TargetKind.MIXED)
                return true;
        }
        return false;
    }

    public int findLine(AbstractInsnNode instruction) {
        if (instruction.getNext() instanceof LineNumberNode lineNode && lineNode.start == instruction)
            return lineNode.line;

        while (!(instruction.getPrevious() instanceof LineNumberNode lineNode))
            instruction = instruction.getPrevious();
        return lineNode.line;
    }

    public int findLineId(LabelNode instruction) {
        int line = findLine(instruction);
        return lineToId.get(line);
    }

    public int getNextLineId() {
        return lineId;
    }
}
