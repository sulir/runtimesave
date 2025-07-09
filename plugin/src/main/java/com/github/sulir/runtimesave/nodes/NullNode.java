package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;

public class NullNode extends ValueNode {
    private static final Mapping mapping = mapping(NullNode.class)
            .constructor(NullNode::new);
    private static NullNode instance;

    public NullNode() {
        if (instance != null)
            throw new IllegalStateException("Multiple null nodes not allowed");
    }

    public static NullNode getInstance() {
        if (instance == null)
            instance = new NullNode();
        return instance;
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }
}
