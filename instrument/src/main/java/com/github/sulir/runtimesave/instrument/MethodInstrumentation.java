package com.github.sulir.runtimesave.instrument;

import com.github.sulir.runtimesave.runtime.Collector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class MethodInstrumentation {
    private static final String COLLECTOR_CLASS = Type.getType(Collector.class).getInternalName();

    private final InsnList instructions;

    public MethodInstrumentation(MethodNode method) {
        this.instructions = method.instructions;
    }

    public void instrument(int everyNthLine) {
        for (AbstractInsnNode node = instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof LineNumberNode lineNode) {
                int lineNumber = lineNode.line;

                if (node.getNext() instanceof FrameNode)
                    node = node.getNext();
                insertCallAfter(node);
            }
        }
    }

    private void insertCallAfter(AbstractInsnNode node) {
        MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKESTATIC, COLLECTOR_CLASS, "collect", "()V");
        instructions.insert(node, call);
    }
}
