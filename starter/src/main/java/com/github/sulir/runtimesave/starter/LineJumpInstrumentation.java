package com.github.sulir.runtimesave.starter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class LineJumpInstrumentation implements Opcodes {
    private final InsnList instructions;

    public LineJumpInstrumentation(MethodNode method) {
        this.instructions = method.instructions;
    }

    public void insert(int line) {
        LineNumberNode lineNode = findLineNode(line);

        LabelNode jumpTarget = new LabelNode();
        instructions.insert(new JumpInsnNode(GOTO, jumpTarget));
        instructions.insert(lineNode, jumpTarget);
    }

    private LineNumberNode findLineNode(int line) {
        for (AbstractInsnNode node : instructions) {
            if (node instanceof LineNumberNode lineNode && lineNode.line == line)
                    return lineNode;
        }

        throw new IllegalArgumentException("Line " + line + " not found");
    }
}
