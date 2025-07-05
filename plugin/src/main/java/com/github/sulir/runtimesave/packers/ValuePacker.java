package com.github.sulir.runtimesave.packers;

import com.github.sulir.runtimesave.nodes.GraphNode;
import com.github.sulir.runtimesave.nodes.ValueNode;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class ValuePacker {
    private final Packer[] packers;
    private Map<GraphNode, NodeData> packableNodes;

    public static ValuePacker fromServiceLoader() {
        ServiceLoader<Packer> loader = ServiceLoader.load(Packer.class);
        Packer[] packers = loader.stream().map(ServiceLoader.Provider::get).toArray(Packer[]::new);
        return new ValuePacker(packers);
    }

    public ValuePacker(Packer[] packers) {
        this.packers = packers;
    }

    public Packer[] getPackers() {
        return packers;
    }

    public GraphNode pack(GraphNode graph) {
        return transform(graph, new Transformation(Packer::canPack, Packer::pack));
    }

    public GraphNode unpack(GraphNode graph) {
        return transform(graph, new Transformation(Packer::canUnpack, Packer::unpack));
    }

    private GraphNode transform(GraphNode value, Transformation transformation) {
        packableNodes = new HashMap<>();
        collectNodeData(value, transformation, new HashSet<>());
        GraphNode transformed = applyTransformations(value, transformation, new HashSet<>());
        packableNodes = null;
        return transformed;
    }

    private void collectNodeData(GraphNode node, Transformation transformation, Set<GraphNode> visited) {
        if (!visited.add(node))
            return;

        if (node instanceof ValueNode value) {
            for (Packer packer : packers) {
                if (transformation.applicable().test(packer, value)) {
                    packableNodes.put(node, new NodeData(packer, new ArrayList<>()));
                    break;
                }
            }
        }

        for (Map.Entry<?, ? extends GraphNode> entry : node.outEdges().entrySet()) {
            GraphNode target = entry.getValue();
            collectNodeData(target, transformation, visited);
            NodeData packableNode = packableNodes.get(target);
            if (packableNode != null)
                packableNode.sources().add(new Edge(entry));
        }
    }

    private GraphNode applyTransformations(GraphNode node, Transformation transformation, Set<GraphNode> visited) {
        visited.add(node);

        NodeData packableNode = packableNodes.get(node);
        if (packableNode != null) {
            ValueNode transformed = transformation.transformer().apply(packableNode.packer(), (ValueNode) node);
            for (Edge source : packableNode.sources())
                source.setTarget(transformed);
            return transformed;
        }

        for (GraphNode target : node.targets())
            if (!visited.contains(target))
                applyTransformations(target, transformation, visited);

        return node;
    }

    private record Transformation(BiPredicate<Packer, ValueNode> applicable,
                                  BiFunction<Packer, ValueNode, ValueNode> transformer) { }

    private record NodeData(Packer packer, List<Edge> sources) { }

    private record Edge(Map.Entry<?, ? extends GraphNode> sourceEntry) {
        @SuppressWarnings("unchecked")
        void setTarget(GraphNode value) {
            ((Map.Entry<?, GraphNode>) sourceEntry).setValue(value);
        }
    }
}
