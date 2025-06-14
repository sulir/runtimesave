package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.List;

public class GraphHasher {
    private final GraphNode root;

    public GraphHasher(GraphNode root) {
        this.root = root;
    }

    public void updateHashes() {
        List<StrongComponent> components = new TarjanScc(root).computeComponents();

        root.traverse(GraphNode::freeze);
        root.traverse(node -> node.setHash(new Hash()));
    }
}
