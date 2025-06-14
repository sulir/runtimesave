package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.ObjectMapper;

import java.util.Collections;
import java.util.SortedMap;

public abstract class GraphNode {
    public SortedMap<?, GraphNode> outEdges() {
        return Collections.emptySortedMap();
    }

    public Iterable<GraphNode> iterate() {
        return outEdges().values();
    }

    public SortedMap<String, Object> getProperties() {
        return ObjectMapper.forClass(getClass()).getProperties(this);
    }
}
