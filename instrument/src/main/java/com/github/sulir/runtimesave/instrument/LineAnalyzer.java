package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

public class LineAnalyzer {
    private int lineId = 0;

    public LineCfg analyze(MethodNode method) throws LineNumberException {
        if (!(method.instructions.getFirst().getNext() instanceof LineNumberNode node))
            throw new LineNumberException();

        LineCfg lineCfg = new LineCfg(method.instructions, lineId);
        int line = node.line;

        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LineNumberNode lineNode) {
                if (lineNode.start != lineNode.getPrevious())
                    throw new LineNumberException();
                line = lineNode.line;
                lineCfg.setLineNumber(lineNode.getPrevious(), line);
            }
            lineCfg.setLineNumber(instruction, line);
        }

        lineId = lineCfg.getNextLineId();
        return lineCfg;
    }
}
