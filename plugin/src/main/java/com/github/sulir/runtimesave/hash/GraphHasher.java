package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.*;

public class GraphHasher {
    private final TarjanScc tarjanScc = new TarjanScc();
    private final ObjectHasher hasher = new ObjectHasher();
    private List<GraphNode> visits;

    public NodeHash assignHashes(GraphNode root) {
        root.traverse(GraphNode::freeze);
        List<StrongComponent> components = tarjanScc.computeComponents(root);

        for (StrongComponent component : components) {
            SccEncoding minEncoding = minimumEncoding(component);
            byte[] minSequenceHash = hasher.hash(minEncoding.sequence());

            for (GraphNode node : component.nodes()) {
                int order = minEncoding.order().get(node);
                hasher.add(minSequenceHash);
                hasher.add(order);
                node.setHash(new NodeHash(hasher.finish()));
            }
        }

        return root.hash();
    }

    private SccEncoding minimumEncoding(StrongComponent component) {
        SccEncoding minEncoding = new SccEncoding(new ArrayList<>(), new HashMap<>());

        for (GraphNode node : component.nodes()) {
            SccEncoding encoding = new SccEncoding(new ArrayList<>(), new HashMap<>(Map.of(node, 0)));
            visits = new ArrayList<>(List.of(node));
            int i = 0;
            boolean tie = false;

            while (i < visits.size()) {
                GraphNode visit = visits.get(i);
                encoding.sequence().add(localHash(visit, component, encoding.order()));

                int diff;
                tie = false;
                if (i >= minEncoding.sequence().size())
                    minEncoding = encoding;
                else if ((diff = encoding.compare(minEncoding, i)) < 0)
                    minEncoding = encoding;
                else if (diff == 0)
                    tie = true;
                else
                    break;
                i++;
            }

            if (tie)
                handleTie(encoding, minEncoding);
        }

        visits = null;
        return minEncoding;
    }

    private void handleTie(SccEncoding encoding, SccEncoding minEncoding) {
        encoding.order().forEach((node, index) -> {
            if (index < minEncoding.order().get(node))
                minEncoding.order().put(node, index);
        });
    }

    private byte[] localHash(GraphNode node, StrongComponent scc, Map<GraphNode, Integer> order) {
        if (scc.contains(node)) {
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

    private record SccEncoding(List<byte[]> sequence, Map<GraphNode, Integer> order) {
        public int compare(SccEncoding other, int index) {
            return Arrays.compare(sequence.get(index), other.sequence.get(index));
        }
    }
}
