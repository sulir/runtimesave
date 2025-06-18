package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.List;

public class GraphHasher {
    private final GraphNode root;

    public GraphHasher(GraphNode root) {
        this.root = root;
    }

    public void assignHashes() {
        root.traverse(GraphNode::freeze);
        List<StrongComponent> components = new TarjanScc(root).computeComponents();

        root.traverse(node -> node.setHash(new NodeHash(new byte[0])));
    }
}
