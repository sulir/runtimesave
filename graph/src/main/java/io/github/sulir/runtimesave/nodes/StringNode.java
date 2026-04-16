package io.github.sulir.runtimesave.nodes;

import io.github.sulir.runtimesave.graph.Mapping;
import io.github.sulir.runtimesave.graph.ValueNode;

public class StringNode extends ValueNode {
    public static final Mapping mapping = mapping(StringNode.class)
            .property("value", String.class, StringNode::getValue)
            .constructor((String v) -> new StringNode(v));

    private String value;

    public StringNode(String value) {
        this.value = value;
    }

    public StringNode() { }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        checkModification();
        this.value = value;
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }
}
