package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.ObjectMapper;
import com.github.sulir.runtimesave.hash.GraphHasher;
import com.github.sulir.runtimesave.hash.NodeHash;

import java.util.*;
import java.util.function.Consumer;

public abstract class GraphNode {
    private NodeHash hash;

    public String label() {
        return ObjectMapper.forClass(getClass()).getLabel();
    }

    public SortedMap<String, Object> properties() {
        return ObjectMapper.forClass(getClass()).getProperties(this);
    }

    public SortedMap<?, GraphNode> outEdges() {
        return Collections.emptySortedMap();
    }

    public Collection<GraphNode> targets() {
        return outEdges().values();
    }

    public void traverse(Consumer<GraphNode> function) {
        traverse(function, new HashSet<>());
    }

    public NodeHash hash() {
        if (hash == null)
            new GraphHasher(this).assignHashes();

        return hash;
    }

    public void setHash(NodeHash hash) {
        this.hash = hash;
    }

    public void freeze() { }

    private void traverse(Consumer<GraphNode> function, Set<GraphNode> visited) {
        function.accept(this);
        visited.add(this);

        for (GraphNode target : targets())
            if (!visited.contains(target))
                target.traverse(function, visited);
    }

    @Override
    public String toString() {
        String nodeInfo = label() + "@" + Integer.toHexString(hashCode()) + "(";
        String properties = properties().values().toString();
        StringBuilder result = new StringBuilder(nodeInfo + properties.substring(1, properties.length() - 1));
        outEdges().forEach((key, target) -> {
            String targetInfo = target.label() + "@" + Integer.toHexString(target.hashCode());
            result.append(", ").append(key).append("->").append(targetInfo);
        });
        return result + ")";
    }
}
