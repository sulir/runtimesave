package com.github.sulir.runtimesave.nodes;

import java.util.SortedMap;
import java.util.TreeMap;

public class FrameNode extends GraphNode {
    private final SortedMap<String, GraphNode> variables = new TreeMap<>();

    public SortedMap<String, GraphNode> getVariables() {
        return variables;
    }

    public GraphNode getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, GraphNode variable) {
        variables.put(name, variable);
    }
}
