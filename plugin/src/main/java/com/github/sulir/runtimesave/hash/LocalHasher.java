package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

class LocalHasher {
    private final ObjectHasher objectHasher;

    LocalHasher(ObjectHasher objectHasher) {
        this.objectHasher = objectHasher;
    }

    void assignLocalHashes(GraphNode graph) {
        graph.traverse(node -> {
            objectHasher.reset()
                    .addString(node.label())
                    .addProperties(node.properties());
            node.forEachEdge((label, target) -> objectHasher.add(label));
            node.setLocalHash(new NodeHash(objectHasher.finish()));
        });
    }
}
