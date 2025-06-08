package com.github.sulir.runtimesave.nodes;

import java.util.HashMap;
import java.util.Map;

public class FrameNode extends GraphNode {
    private final Map<String, GraphNode> variables = new HashMap<>();

    public void addVariable(String name, GraphNode node) {
        variables.put(name, node);
    }

    public GraphNode getVariable(String name) {
        return variables.get(name);
    }
}
