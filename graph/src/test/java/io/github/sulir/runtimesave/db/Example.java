package io.github.sulir.runtimesave.db;

import io.github.sulir.runtimesave.graph.NodeFactory;
import io.github.sulir.runtimesave.graph.ReflectionReader;
import io.github.sulir.runtimesave.graph.ValueNode;
import io.github.sulir.runtimesave.hash.AcyclicGraph;
import io.github.sulir.runtimesave.hash.GraphHasher;
import io.github.sulir.runtimesave.hash.GraphIdHasher;
import io.github.sulir.runtimesave.nodes.ObjectNode;

import javax.swing.*;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public class Example {
    public static void main(String[] args) {
        JColorChooser chooser = new JColorChooser();
        writeObject(chooser, "JColorChooser");
        writeObject(chooser, "JColorChooser copy");
        writeObject(ZoneId.getAvailableZoneIds().stream().map(z -> ZoneId.of(z).getRules()).toList(), "ZoneId");
    }

    private static void writeObject(Object object, String name) {
        System.out.println(name + ":");
        NodeFactory factory = new NodeFactory();
        GraphHasher hasher = new GraphHasher();
        GraphIdHasher idHasher = new GraphIdHasher();
        HashedDb database = new HashedDb(DbConnection.getInstance(), factory);
        DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
        Metadata metadata = new Metadata(DbConnection.getInstance());

        ValueNode graph = new ReflectionReader().read(object);
        AtomicReference<AcyclicGraph> dag = new AtomicReference<>();
        time(() -> dag.set(AcyclicGraph.multiCondensationOf(graph)), "dag");
        time(() -> hasher.assignHashes(dag.get()), "hash");
        time(() -> idHasher.assignIdHashes(graph), "id-hash");

        dbIndex.createIndexes();
        time(() -> database.write(dag.get()), "write");
        time(() -> metadata.addNote(graph.idHash(), name), "meta");

        database.read(graph.hash(), ObjectNode.class);
        System.out.println();
    }

    public static void time(Runnable measured, String name) {
        long time = System.currentTimeMillis();
        measured.run();
        System.out.println(name + ": " + (System.currentTimeMillis() - time) + " ms");
    }
}
