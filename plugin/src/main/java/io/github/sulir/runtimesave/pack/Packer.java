package io.github.sulir.runtimesave.pack;

import io.github.sulir.runtimesave.graph.ValueNode;

public interface Packer {
    boolean canPack(ValueNode node);
    ValueNode pack(ValueNode node);
    boolean canUnpack(ValueNode node);
    ValueNode unpack(ValueNode node);
}
