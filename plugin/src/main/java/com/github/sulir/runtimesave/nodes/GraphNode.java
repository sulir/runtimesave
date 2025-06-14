package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.ObjectMapper;
import com.github.sulir.runtimesave.hash.GraphHasher;
import com.github.sulir.runtimesave.hash.Hash;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Consumer;

public abstract class GraphNode {
    private Hash hash;

    public SortedMap<String, Object> properties() {
        return ObjectMapper.forClass(getClass()).getProperties(this);
    }

    public SortedMap<?, GraphNode> outEdges() {
        return Collections.emptySortedMap();
    }

    public Iterable<GraphNode> iterate() {
        return outEdges().values();
    }

    public void traverse(Consumer<GraphNode> function) {
        traverse(function, new HashSet<>());
    }

    public Hash hash() {
        if (hash == null)
            new GraphHasher(this).updateHashes();

        return hash;
    }

    public void setHash(Hash hash) {
        this.hash = hash;
    }

    public void freeze() { }

    private void traverse(Consumer<GraphNode> function, Set<GraphNode> visited) {
        function.accept(this);
        visited.add(this);

        for (GraphNode target : iterate())
            if (!visited.contains(target))
                target.traverse(function, visited);
    }
}
