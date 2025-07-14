package com.github.sulir.runtimesave.packing;

import com.github.sulir.runtimesave.graph.Edge;
import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.ValueNode;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class ValuePacker {
    private final Packer[] packers;

    public static ValuePacker fromServiceLoader() {
        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        ClassLoader pluginClassLoader = ValuePacker.class.getClassLoader();
        try {
            thread.setContextClassLoader(pluginClassLoader);
            ServiceLoader<Packer> loader = ServiceLoader.load(Packer.class);
            Packer[] packers = loader.stream().map(ServiceLoader.Provider::get).toArray(Packer[]::new);
            return new ValuePacker(packers);
        } finally {
            thread.setContextClassLoader(originalClassLoader);
        }
    }

    public ValuePacker(Packer[] packers) {
        this.packers = packers;
    }

    public Packer[] getAllPackers() {
        return packers;
    }

    public GraphNode pack(GraphNode graph) {
        return transform(graph, new Transformation(Packer::canPack, Packer::pack));
    }

    public GraphNode unpack(GraphNode graph) {
        return transform(graph, new Transformation(Packer::canUnpack, Packer::unpack));
    }

    private GraphNode transform(GraphNode value, Transformation transformation) {
        Map<GraphNode, NodeData> packable = new HashMap<>();
        collectNodeData(value, transformation, new HashSet<>(), packable);
        return applyTransformations(value, transformation, new HashSet<>(), packable);
    }

    private void collectNodeData(GraphNode node, Transformation transformation,
                                 Set<GraphNode> visited, Map<GraphNode, NodeData> packable) {
        if (!visited.add(node))
            return;

        if (node instanceof ValueNode value) {
            for (Packer packer : packers) {
                if (transformation.applicable().test(packer, value)) {
                    packable.put(node, new NodeData(packer, new ArrayList<>()));
                    break;
                }
            }
        }

        node.edges().forEach(edge -> {
            collectNodeData(edge.target(), transformation, visited, packable);
            NodeData packableNode = packable.get(edge.target());
            if (packableNode != null)
                packableNode.sources().add(edge);
        });
    }

    private GraphNode applyTransformations(GraphNode node, Transformation transformation,
                                           Set<GraphNode> visited, Map<GraphNode, NodeData> packable) {
        if (!visited.add(node))
            return node;
        node.forEachTarget(target -> applyTransformations(target, transformation, visited, packable));

        NodeData packableNode = packable.get(node);
        if (packableNode != null) {
            ValueNode transformed = transformation.transformer().apply(packableNode.packer(), (ValueNode) node);
            for (Edge sourceEdge : packableNode.sources())
                sourceEdge.source().setTarget(sourceEdge.label(), transformed);
            return transformed;
        }
        return node;
    }

    private record Transformation(BiPredicate<Packer, ValueNode> applicable,
                                  BiFunction<Packer, ValueNode, ValueNode> transformer) { }

    private record NodeData(Packer packer, List<Edge> sources) { }
}
