package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GraphHasher {
    private final TarjanScc tarjanScc = new TarjanScc();
    private final ObjectHasher hasher = new ObjectHasher();

    public NodeHash assignHashes(GraphNode root) {
        root.traverse(GraphNode::freeze);
        List<StrongComponent> components = tarjanScc.computeComponents(root);

        for (StrongComponent scc : components) {
            SccEncoding minEncoding = scc.nodes().stream()
                    .map(node -> new Traversal(node, scc))
                    .min(Comparator.naturalOrder()).orElseThrow()
                    .encode();
            byte[] minSequenceHash = hasher.hash(minEncoding.hashSequence());

            for (GraphNode node : scc.nodes()) {
                node.setHash(new NodeHash(hasher.reset()
                        .add(minSequenceHash)
                        .add(minEncoding.order().get(node))
                        .finish()));
            }
        }
        return root.hash();
    }

    private class Traversal implements Comparable<Traversal> {
        private final StrongComponent scc;
        private final List<byte[]> hashes = new ArrayList<>();
        private final Map<GraphNode, Integer> order = new HashMap<>();
        private final List<GraphNode> visits = new ArrayList<>();

        Traversal(GraphNode first, StrongComponent scc) {
            this.scc = scc;
            order.put(first, 0);
            visits.add(first);
        }

        @Override
        public int compareTo(@NotNull Traversal other) {
            byte[] hash;
            for (int i = 0; (hash = hashAt(i)) != null; i++) {
                int comparison = Arrays.compare(hash, other.hashAt(i));
                if (comparison != 0)
                    return comparison;
            }
            return handleTie(other);
        }

        SccEncoding encode() {
            hashAt(Integer.MAX_VALUE);
            return new SccEncoding(hashes, order);
        }

        private byte[] hashAt(int index) {
            while (hashes.size() <= index && hashes.size() < visits.size())
                hashes.add(localHash(visits.get(hashes.size())));

            return index < hashes.size() ? hashes.get(index) : null;
        }

        private byte[] localHash(GraphNode node) {
            if (!scc.contains(node))
                return node.hash().getBytes();

            hasher.reset().add(node.label()).add(node.properties());

            node.outEdges().forEach((property, target) -> {
                int targetOrder = order.computeIfAbsent(target, t -> {
                    visits.add(t);
                    return visits.size() - 1;
                });
                hasher.add(property).add(targetOrder);
            });

            return hasher.finish();
        }

        private int handleTie(Traversal other) {
            other.order.forEach((node, otherIndex) -> order.merge(node, otherIndex, Math::min));
            return -1;
        }
    }

    private record SccEncoding(List<byte[]> hashSequence, Map<GraphNode, Integer> order) {
    }
}
