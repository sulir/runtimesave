package io.github.sulir.runtimesave.nodes;

import io.github.sulir.runtimesave.graph.GraphNode;
import io.github.sulir.runtimesave.graph.Mapping;
import io.github.sulir.runtimesave.graph.ValueNode;

import java.util.SortedMap;
import java.util.TreeMap;

public class FrameNode extends GraphNode {
    public static final Mapping mapping = mapping(FrameNode.class)
            .edges("HAS_VARIABLE", "name", String.class, ValueNode.class, node -> node.variables)
            .constructor(FrameNode::new);

    private final SortedMap<String, ValueNode> variables = new TreeMap<>();

    public ValueNode getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, ValueNode variable) {
        checkModification();
        variables.put(name, variable);
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }
}
