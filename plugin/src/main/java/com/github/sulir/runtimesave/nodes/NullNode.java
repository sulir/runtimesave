package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.graph.Mapping;
import com.github.sulir.runtimesave.graph.ValueNode;

public class NullNode extends ValueNode {
    private static final Mapping mapping = mapping(NullNode.class)
            .constructor(NullNode::new);
    private static final NullNode instance = new NullNode();

    public static NullNode getInstance() {
        return instance;
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }
}
