package com.github.sulir.runtimesave.nodes;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;

public abstract class ValueNode extends GraphNode {
    @Override
    public SortedMap<?, ValueNode> outEdges() {
        return Collections.emptySortedMap();
    }

    @Override
    public Collection<ValueNode> targets() {
        return outEdges().values();
    }
}
