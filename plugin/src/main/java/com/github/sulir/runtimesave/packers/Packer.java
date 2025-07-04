package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.nodes.ValueNode;

public interface Packer {
    boolean canPack(ValueNode node);
    ValueNode pack(ValueNode node);
    boolean canUnpack(ValueNode node);
    ValueNode unpack(ValueNode node);
}
