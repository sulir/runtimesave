package io.github.sulir.runtimesave.nodes;

import io.github.sulir.runtimesave.graph.Mapping;
import io.github.sulir.runtimesave.graph.ValueNode;

public class StringNode extends ValueNode {
    public static final Mapping mapping = mapping(StringNode.class)
            .property("value", String.class, StringNode::getValue)
            .constructor(StringNode::new);

    private final String value;

    public StringNode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }
}
