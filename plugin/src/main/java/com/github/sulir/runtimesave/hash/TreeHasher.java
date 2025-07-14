package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class TreeHasher {
    private final ObjectHasher objectHasher;
    private Map<StrongComponent, SccData> sccToData;

    TreeHasher(ObjectHasher objectHasher) {
        this.objectHasher = objectHasher;
    }

    void assignHashes(AcyclicGraph dag) {
        sccToData = new HashMap<>(dag.getComponentCount());
        markTreeNodes(dag);
        assignHashesToMarked(dag);
        sccToData = null;
    }

    private void markTreeNodes(AcyclicGraph dag) {
        for (StrongComponent scc : dag.topoOrder()) {
            SccData data = getData(scc);
            if (!scc.isTrivial())
                for (StrongComponent pathScc : data.path)
                    getData(pathScc).tree = false;

            for (StrongComponent target : scc.targets()) {
                SccData targetData = getData(target);
                for (StrongComponent pathScc : data.path) {
                    if (!targetData.path.add(pathScc))
                        getData(pathScc).tree = false;
                }
            }
            data.path = null;
        }
    }

    private void assignHashesToMarked(AcyclicGraph dag) {
        for (StrongComponent scc : dag.reverseTopoOrder()) {
            if (getData(scc).tree) {
                GraphNode node = scc.getSoleNode();
                objectHasher.reset().addHash(node.localHash());
                node.forEachEdge((label, target) -> objectHasher.addHash(target.hash()));
                node.setHash(new NodeHash(objectHasher.finish()));
            }
        }
    }

    private static class SccData {
        boolean tree = true;
        Set<StrongComponent> path;

        SccData(StrongComponent scc) {
            path = new HashSet<>(Set.of(scc));
        }
    }

    private SccData getData(StrongComponent scc) {
        return sccToData.computeIfAbsent(scc, s -> new SccData(scc));
    }
}
