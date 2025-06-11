package com.github.sulir.runtimesave.nodes;

import java.util.*;

public class FrameNode extends GraphNode {
    private final SortedMap<String, GraphNode> variables = new TreeMap<>();

    public GraphNode getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, GraphNode node) {
        variables.put(name, node);
    }

    public Map<String, GraphNode> getVariables() {
        return variables;
    }
}
