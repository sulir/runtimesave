package com.github.sulir.runtimesave.nodes;

import java.util.*;

public class FrameNode extends GraphNode {
    private SortedMap<String, ValueNode> variables = new TreeMap<>();

    @Override
    public SortedMap<String, ValueNode> outEdges() {
        return variables;
    }

    @Override
    public Collection<ValueNode> targets() {
        return variables.values();
    }

    public ValueNode getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, ValueNode variable) {
        variables.put(name, variable);
    }

    @Override
    public void freeze() {
        variables = Collections.unmodifiableSortedMap(variables);
    }
}
