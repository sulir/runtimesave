package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

public class LineCfgCreator {
    private int lineId = 0;

    public LineCfg create(MethodNode method, String className) throws LineNumberException, AnalyzerException {
        if (method.instructions.size() < 2 || !(method.instructions.getFirst().getNext() instanceof LineNumberNode))
            throw new LineNumberException();

        LineCfg lineCfg = new LineCfg(method.instructions, lineId);
        buildLineNumbers(method.instructions, lineCfg);
        buildControlFlowGraph(method, className, lineCfg);
        return lineCfg;
    }

    private void buildLineNumbers(InsnList instructions, LineCfg lineCfg) throws LineNumberException {
        int line = ((LineNumberNode) instructions.getFirst().getNext()).line;

        for (AbstractInsnNode instruction : instructions) {
            if (instruction instanceof LineNumberNode lineNode) {
                if (lineNode.start != lineNode.getPrevious())
                    throw new LineNumberException();
                line = lineNode.line;
                lineCfg.setLineNumber(lineNode.getPrevious(), line);
            }
            lineCfg.setLineNumber(instruction, line);
        }

        lineId = lineCfg.getNextLineId();
    }

    private void buildControlFlowGraph(MethodNode method, String className, LineCfg lineCfg) throws AnalyzerException {
        new Analyzer<>(new BasicInterpreter()) {
            @Override
            protected void newControlFlowEdge(int fromIndex, int toIndex) {
                lineCfg.addEdge(fromIndex, toIndex);
            }

            @Override
            protected boolean newControlFlowExceptionEdge(int fromIndex, int toIndex) {
                lineCfg.addEdge(fromIndex, toIndex);
                return true;
            }
        }.analyze(className, method);
    }
}
