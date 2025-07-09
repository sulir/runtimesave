package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.hash.NodeHash;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class GraphNode {
    private NodeHash hash;
    private NodeHash idHash;

    protected static <T extends GraphNode> Mapping.Builder<T> mapping(Class<T> clazz) {
        return new Mapping.Builder<>(clazz);
    }

    public abstract Mapping getMapping();

    public String label() {
        return getMapping().label();
    }

    public NodeProperty[] properties() {
        Mapping.PropertySpec[] propertySpecs = getMapping().properties();
        NodeProperty[] props = new NodeProperty[propertySpecs.length];
        for (int i = 0; i < propertySpecs.length; i++) {
            Mapping.PropertySpec mapping = propertySpecs[i];
            props[i] = new NodeProperty(mapping.key(), mapping.getter().apply(this));
        }
        return props;
    }

    public Stream<? extends GraphNode> targets() {
        return getMapping().edgeMap().apply(this).values().stream();
    }

    public void forEachTarget(Consumer<GraphNode> action) {
        getMapping().edgeMap().apply(this).values().forEach(action);
    }

    public void setTarget(Object edgeLabel, GraphNode target) {
        checkModification();
        if (!getMapping().relation().propertyType().isInstance(edgeLabel))
            throw new IllegalArgumentException("Incompatible edge label type: " + edgeLabel.getClass().getName());
        if (!getMapping().relation().targetType().isInstance(target))
            throw new IllegalArgumentException("Incompatible target type: " + target.getClass().getName());

        @SuppressWarnings("unchecked")
        SortedMap<Object, GraphNode> edges = (SortedMap<Object, GraphNode>) getMapping().edgeMap().apply(this);
        edges.put(edgeLabel, target);
    }

    public Stream<Edge> edges() {
        return getMapping().edgeMap().apply(this).entrySet().stream()
                .map(edge -> new Edge(this, edge.getKey(), edge.getValue()));
    }

    public int edgeCount() {
        return getMapping().edgeMap().apply(this).size();
    }

    public void forEachEdge(BiConsumer<Object, GraphNode> action) {
        getMapping().edgeMap().apply(this).forEach(action);
    }

    public void traverse(Consumer<GraphNode> function) {
        traverse(function, new HashSet<>());
    }

    public NodeHash hash() {
        if (hash == null)
            throw new IllegalStateException("Hash not yet computed");
        return hash;
    }

    public boolean hasHash() {
        return hash != null;
    }

    public void setHash(NodeHash hash) {
        this.hash = hash;
    }

    public NodeHash idHash() {
        if (idHash == null)
            throw new IllegalStateException("ID-hash not yet computed");
        return idHash;
    }

    public void setIdHash(NodeHash idHash) {
        this.idHash = idHash;
    }

    @Override
    public String toString() {
        Function<GraphNode, String> shortId = n -> n.label() + ":" + Integer.toHexString(n.hashCode() & 0xFFFF);
        String properties = Arrays.stream(properties())
                .map(property -> property.value().toString())
                .collect(Collectors.joining(", "));
        int edgeLimit = 8;
        var data = new Object() {
            int edgeCount = 0;
        };
        String edges = edges()
                .map(edge -> edge.label() + "->" + shortId.apply(edge.target()))
                .map(string -> (++data.edgeCount > edgeLimit) ? "..." : string)
                .limit(edgeLimit + 1)
                .collect(Collectors.joining(", "));
        String details = Stream.of(properties, edges)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        return shortId.apply(this) + (details.isEmpty() ? "" : "(" + details + ")");
    }

    protected void checkModification() {
        if (hash != null || idHash != null)
            throw new IllegalStateException("Cannot modify node after hash or ID-hash has been set");
    }

    private void traverse(Consumer<GraphNode> function, Set<GraphNode> visited) {
        if (!visited.add(this))
            return;
        function.accept(this);

        for (GraphNode target : getMapping().edgeMap().apply(this).values())
            target.traverse(function, visited);
    }
}
