package com.github.sulir.runtimesave.nodes;

import com.github.sulir.runtimesave.ObjectMapper;
import com.github.sulir.runtimesave.hash.NodeHash;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return hash;
    }

    public void setHash(NodeHash hash) {
        this.hash = hash;
    }

    public void freeze() { }

    @Override
    public String toString() {
        String properties = properties().values().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        int edgeLimit = 8;
        int[] edgeCount = {0};
        String edges = outEdges().entrySet().stream()
                .map(entry -> entry.getKey() + "->" + entry.getValue().shortId())
                .map(string -> (++edgeCount[0] > edgeLimit) ? "..." : string)
                .limit(edgeLimit + 1)
                .collect(Collectors.joining(", "));
        String details = Stream.of(properties, edges)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        return shortId() + (details.isEmpty() ? "" : "(" + details + ")");
    }

    private void traverse(Consumer<GraphNode> function, Set<GraphNode> visited) {
        function.accept(this);
        visited.add(this);

        for (GraphNode target : targets())
            if (!visited.contains(target))
                target.traverse(function, visited);
    }

    private String shortId() {
        return label() + ":" + Integer.toHexString(hashCode() & 0xFFFF);
    }
}
