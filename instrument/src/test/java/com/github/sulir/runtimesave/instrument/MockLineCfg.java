package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MockLineCfg extends LineCfg {
    public static class Edge {
        private final AbstractInsnNode from;
        private final AbstractInsnNode to;

        public Edge(AbstractInsnNode from, AbstractInsnNode to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge edge))
                return false;
            return Objects.equals(from, edge.from) && Objects.equals(to, edge.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return (from + "->" + to).replace(from.getClass().getPackageName() + '.', "");
        }
    }

    private final Set<Edge> edges = new HashSet<>();

    public MockLineCfg() {
        super(new InsnList(), new AtomicInteger(-1));
    }

    @Override
    public void addEdge(AbstractInsnNode from, AbstractInsnNode to) {
        edges.add(new Edge(from, to));
    }

    public Set<Edge> edges() {
        return edges;
    }
}
