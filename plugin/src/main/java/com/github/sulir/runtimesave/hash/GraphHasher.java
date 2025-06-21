package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.*;

public class GraphHasher {
    private final TarjanScc tarjanScc = new TarjanScc();
    private final ObjectHasher hasher = new ObjectHasher();

    public NodeHash assignHashes(GraphNode root) {
        root.traverse(GraphNode::freeze);
        List<StrongComponent> components = tarjanScc.computeComponents(root);

        for (StrongComponent scc : components) {
            SccEncoding minEncoding = minimumEncoding(scc);
            byte[] minSequenceHash = hasher.hash(minEncoding.sequence());

            for (GraphNode node : scc.nodes()) {
                int order = minEncoding.order().get(node);
                hasher.reset();
                hasher.add(minSequenceHash);
                hasher.add(order);
                node.setHash(new NodeHash(hasher.finish()));
            }
        }

        return root.hash();
    }

    private SccEncoding minimumEncoding(StrongComponent scc) {
        Iterator<GraphNode> nodes = scc.nodes().iterator();
        Traversal minTraversal = new Traversal(nodes.next(), scc);

        while (nodes.hasNext()) {
            Traversal traversal = new Traversal(nodes.next(), scc);
            minTraversal = minTraversal.competeWith(traversal);
        }

        return minTraversal.encode();
    }

    private class Traversal {
        private final StrongComponent scc;
        private final List<byte[]> sequence = new ArrayList<>();
        private final Map<GraphNode, Integer> order = new HashMap<>();
        private final List<GraphNode> visits = new ArrayList<>();

        public Traversal(GraphNode first, StrongComponent scc) {
            this.scc = scc;
            order.put(first, 0);
            visits.add(first);
        }

        Traversal competeWith(Traversal other) {
            int i = 0;
            while (true) {
                int comparison = Arrays.compare(sequenceAt(i), other.sequenceAt(i));

                if (comparison < 0)
                    return this;
                else if (comparison > 0)
                    return other;
                else if (sequenceAt(i + 1) == null)
                    return handleTie(other);
                else
                    i++;
            }
        }

        SccEncoding encode() {
            while (sequence.size() < visits.size())
                sequence.add(localHash(visits.get(sequence.size())));

            return new SccEncoding(sequence, order);
        }

        private byte[] sequenceAt(int sequenceIndex) {
            if (sequenceIndex >= visits.size())
                return null;

            while (sequence.size() <= sequenceIndex)
                sequence.add(localHash(visits.get(sequence.size())));

            return sequence.get(sequenceIndex);
        }

        private byte[] localHash(GraphNode node) {
            if (scc.contains(node)) {
                hasher.reset();
                hasher.add(node.label());
                hasher.add(node.properties());

                node.outEdges().forEach((property, target) -> {
                    if (!order.containsKey(target)) {
                        visits.add(target);
                        order.put(target, visits.size() - 1);
                    }
                    hasher.add(property);
                    hasher.add(order.get(target));
                });

                return hasher.finish();
            } else {
                return node.hash().getBytes();
            }
        }

        private Traversal handleTie(Traversal other) {
            other.order.forEach((node, otherIndex) -> {
                if (otherIndex < order.get(node))
                    order.put(node, otherIndex);
            });
            return this;
        }
    }

    private record SccEncoding(List<byte[]> sequence, Map<GraphNode, Integer> order) {  }
}
