package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class ControlFlowAnalyzer {
    public void analyze(MethodNode method, LineCfg cfg) {
        List<AbstractInsnNode> jsrSuccessors = new ArrayList<>(0);
        List<AbstractInsnNode> rets = new ArrayList<>(0);

        for (AbstractInsnNode instruction : method.instructions)
            processInstruction(instruction, jsrSuccessors, rets, cfg);

        addRetEdges(jsrSuccessors, rets, cfg);
        addExceptionEdges(method, cfg);
    }

    private void processInstruction(AbstractInsnNode instruction, List<AbstractInsnNode> jsrSuccessors,
                                    List<AbstractInsnNode> rets, LineCfg cfg) {
        int opcode = instruction.getOpcode();

        if (instruction instanceof JumpInsnNode jump) {
            cfg.addEdge(instruction, jump.label);

            if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && instruction.getNext() != null)
                cfg.addEdge(instruction, instruction.getNext());

            if (opcode == Opcodes.JSR && instruction.getNext() != null)
                jsrSuccessors.add(instruction.getNext());
        } else if (opcode == Opcodes.RET) {
            rets.add(instruction);
        } else if (instruction instanceof TableSwitchInsnNode tableSwitch) {
            for (LabelNode label : tableSwitch.labels)
                cfg.addEdge(instruction, label);
            cfg.addEdge(instruction, tableSwitch.dflt);
        } else if (instruction instanceof LookupSwitchInsnNode lookupSwitch) {
            for (LabelNode label : lookupSwitch.labels)
                cfg.addEdge(instruction, label);
            cfg.addEdge(instruction, lookupSwitch.dflt);
        } else if (!(opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) && opcode != Opcodes.ATHROW) {
            if (instruction.getNext() != null)
                cfg.addEdge(instruction, instruction.getNext());
        }
    }

    private void addRetEdges(List<AbstractInsnNode> jsrSuccessors, List<AbstractInsnNode> rets, LineCfg cfg) {
        for (AbstractInsnNode ret : rets)
            for (AbstractInsnNode jsrSuccessor : jsrSuccessors)
                cfg.addEdge(ret, jsrSuccessor);
    }

    private void addExceptionEdges(MethodNode method, LineCfg cfg) {
        for (TryCatchBlockNode block : method.tryCatchBlocks)
            for (AbstractInsnNode node = block.start; node != block.end; node = node.getNext())
                if (node.getOpcode() != -1)
                    cfg.addEdge(node, block.handler);
    }
}
