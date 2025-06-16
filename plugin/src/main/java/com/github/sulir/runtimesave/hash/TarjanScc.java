package com.github.sulir.runtimesave.hash;

import com.github.sulir.runtimesave.nodes.GraphNode;

import java.util.*;

public class TarjanScc {
    private final GraphNode root;
    private Map<GraphNode, NodeData> nodeToData;
    private int index;
    private Deque<GraphNode> stack;
    private List<StrongComponent> result;

    public TarjanScc(GraphNode root) {
        this.root = root;
    }

    public List<StrongComponent> computeComponents() {
        nodeToData = new HashMap<>();
        index = 0;
        stack = new ArrayDeque<>();
        result = new ArrayList<>();
        strongConnect(root);
        return result;
    }

    private void strongConnect(GraphNode node) {
        NodeData data = getData(node);
        data.index = data.lowLink = index++;
        stack.push(node);
        data.onStack = true;

        for (GraphNode targetNode : node.targets()) {
            NodeData target = getData(targetNode);
            if (target.index == NodeData.UNVISITED) {
                strongConnect(targetNode);
                data.lowLink = Math.min(data.lowLink, target.lowLink);
            } else if (target.onStack) {
                data.lowLink = Math.min(data.lowLink, target.index);
            }
        }

        if (data.lowLink == data.index) {
            StrongComponent component = new StrongComponent();
            GraphNode added;
            do {
                added = stack.pop();
                getData(added).onStack = false;
                component.add(added);
            } while (added != node);
            result.add(component);
        }
    }

    private static class NodeData {
        static final int UNVISITED = -1;

        int index = UNVISITED;
        int lowLink;
        boolean onStack;
    }

    private NodeData getData(GraphNode node) {
        return nodeToData.computeIfAbsent(node, n -> new NodeData());
    }
}
