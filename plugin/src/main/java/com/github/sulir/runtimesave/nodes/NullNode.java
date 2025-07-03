package com.github.sulir.runtimesave.nodes;

public class NullNode extends ValueNode {
    private static final NullNode instance = new NullNode();

    public static NullNode getInstance() {
        return instance;
    }
}
