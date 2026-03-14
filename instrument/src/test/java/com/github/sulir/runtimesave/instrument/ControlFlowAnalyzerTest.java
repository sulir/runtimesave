package com.github.sulir.runtimesave.instrument;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlFlowAnalyzerTest {
    private final MethodNode method = new MethodNode(0, "m", "()V", null, null);
    private final ControlFlowAnalyzer analyzer = new ControlFlowAnalyzer();
    private final MockLineCfg cfg = new MockLineCfg();

    @Test
    void emptyMethodHasNoEdges() {
        analyzer.analyze(method, cfg);
        assertEquals(Set.of(), cfg.edges());
    }

    @Test
    void oneInstructionMethodHasNoEdges() {
        add(new InsnNode(Opcodes.RETURN));

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(), cfg.edges());
    }

    @Test
    void sequentialMethodHasEdges() {
        var one = new InsnNode(Opcodes.ICONST_1);
        var store = new VarInsnNode(Opcodes.ISTORE, 0);
        var returnIns = new InsnNode(Opcodes.RETURN);
        add(one, store, returnIns);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(one, store), edge(store, returnIns)), cfg.edges());
    }

    @Test
    void unconditionalJumpHasOneOutEdge() {
        var label = new LabelNode();
        var gotoIns = new JumpInsnNode(Opcodes.GOTO, label);
        add(label, gotoIns);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(label, gotoIns), edge(gotoIns, label)), cfg.edges());
    }

    @Test
    void conditionalJumpHasTwoOutEdges() {
        var label = new LabelNode();
        var ifEq = new JumpInsnNode(Opcodes.IFEQ, label);
        var returnIns = new InsnNode(Opcodes.RETURN);
        add(label, ifEq, returnIns);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(label, ifEq), edge(ifEq, label), edge(ifEq, returnIns)), cfg.edges());
    }

    @Test
    void conditionalJumpAtEndHasOneOutEdge() {
        var label = new LabelNode();
        var ifEq = new JumpInsnNode(Opcodes.IFEQ, label);
        add(label, ifEq);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(label, ifEq), edge(ifEq, label)), cfg.edges());
    }

    @Test
    void tableSwitchHasEdgesForCases() {
        var case1 = new LabelNode();
        var case2 = new LabelNode();
        var defaultLabel = new LabelNode();
        var tableSwitch = new TableSwitchInsnNode(1, 2, defaultLabel, case1, case2);
        add(case1, case2, tableSwitch, defaultLabel);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(case1, case2), edge(case2, tableSwitch), edge(tableSwitch, case1),
                edge(tableSwitch, case2), edge(tableSwitch, defaultLabel)), cfg.edges());
    }

    @Test
    void lookupSwitchHasEdgesForCases() {
        var case1 = new LabelNode();
        var case2 = new LabelNode();
        var defaultLabel = new LabelNode();
        var tableSwitch = new LookupSwitchInsnNode(defaultLabel, new int[]{1, 2}, new LabelNode[]{case1, case2});
        add(defaultLabel, case1, case2, tableSwitch);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(defaultLabel, case1), edge(case1, case2), edge(case2, tableSwitch),
                edge(tableSwitch, case1), edge(tableSwitch, case2), edge(tableSwitch, defaultLabel)), cfg.edges());
    }

    @Test
    void retJumpsAfterAllJsr() {
        var label = new LabelNode();
        var jsr1 = new JumpInsnNode(Opcodes.JSR, label);
        var jsr2 = new JumpInsnNode(Opcodes.JSR, label);
        var ret = new VarInsnNode(Opcodes.RET, 0);
        var jsr3 = new JumpInsnNode(Opcodes.JSR, label);
        add(jsr1, jsr2, label, ret, jsr3);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(jsr1, label), edge(jsr2, label), edge(label, ret),
                edge(ret, jsr2), edge(ret, label), edge(jsr3, label)), cfg.edges());
    }

    @Test
    void allInstructionsInTryBlockJumpToHandler() {
        var tryStart = new LabelNode();
        var handler = new LabelNode();
        var tryBlock = new TryCatchBlockNode(tryStart, handler, handler, null);
        var newIns = new TypeInsnNode(Opcodes.NEW, "test/Exception");
        var throwIns = new InsnNode(Opcodes.ATHROW);
        method.tryCatchBlocks.add(tryBlock);
        add(tryStart, newIns, throwIns, handler);

        analyzer.analyze(method, cfg);
        assertEquals(Set.of(edge(tryStart, newIns), edge(newIns, throwIns),
                edge(newIns, handler), edge(throwIns, handler)), cfg.edges());
    }

    private void add(AbstractInsnNode... instructions) {
        for (AbstractInsnNode instruction : instructions)
            method.instructions.add(instruction);
    }

    private static MockLineCfg.Edge edge(AbstractInsnNode from, AbstractInsnNode to) {
        return new MockLineCfg.Edge(from, to);
    }
}