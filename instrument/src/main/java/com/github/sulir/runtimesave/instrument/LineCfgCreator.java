package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.HashMap;
import java.util.Map;

public class LineCfgCreator {
    public static final int NO_LINE = -1;

    private int lineId = 0;

    public LineCfg create(MethodNode method, String className) {
        LineCfg lineCfg = new LineCfg(method.instructions, lineId);
        buildLineNumbers(method, lineCfg);
        buildControlFlowGraph(method, className, lineCfg);
        return lineCfg;
    }

    private void buildLineNumbers(MethodNode method, LineCfg lineCfg) {
        Map<LabelNode, Integer> labelLines = new HashMap<>();

        for (AbstractInsnNode instruction : method.instructions)
            if (instruction instanceof LineNumberNode lineNode)
                labelLines.put(lineNode.start, lineNode.line);

        int line = NO_LINE;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label)
                line = labelLines.getOrDefault(label, line);

            lineCfg.setLineNumber(instruction, line);
        }

        lineId = lineCfg.getNextLineId();
    }

    private void buildControlFlowGraph(MethodNode method, String className, LineCfg lineCfg) {
        Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter()) {
            @Override
            protected void newControlFlowEdge(int fromIndex, int toIndex) {
                lineCfg.addEdge(fromIndex, toIndex);
            }

            @Override
            protected boolean newControlFlowExceptionEdge(int fromIndex, int toIndex) {
                lineCfg.addEdge(fromIndex, toIndex);
                return true;
            }
        };

        try {
            analyzer.analyze(className, method);
        } catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }
    }
}
