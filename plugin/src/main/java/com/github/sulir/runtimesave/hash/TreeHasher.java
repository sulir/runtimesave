package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.graph.GraphNode;

import java.util.*;

public class TreeHasher {
    private final ObjectHasher hasher = new ObjectHasher();
    private Map<StrongComponent, SccData> sccToData;

    public void assignHashes(AcyclicGraph dag) {
        sccToData = new IdentityHashMap<>(dag.getComponentCount());
        markTreeNodes(dag);
        computeHashes(dag);
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

    private void computeHashes(AcyclicGraph dag) {
        for (StrongComponent scc : dag.reverseTopoOrder()) {
            if (getData(scc).tree) {
                GraphNode node = scc.getSoleNode();
                hasher.reset().add(node.label()).add(node.properties());
                node.forEachEdge((label, target) -> hasher.add(label).add(target.hash()));
                node.setHash(new NodeHash(hasher.finish()));
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
