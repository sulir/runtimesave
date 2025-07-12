package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.SourceLocation;
import com.github.sulir.runtimesave.graph.NodeFactory;
import com.github.sulir.runtimesave.graph.ReflectionReader;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.hash.AcyclicGraph;
import com.github.sulir.runtimesave.hash.GraphHasher;
import com.github.sulir.runtimesave.hash.GraphIdHasher;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.packing.ValuePacker;

import javax.swing.*;
import java.util.function.Supplier;

public class DbExample {
    public static void main(String[] args) {
        ValuePacker packer = ValuePacker.fromServiceLoader();
        NodeFactory factory = new NodeFactory(packer);
        GraphHasher hasher = new GraphHasher();
        GraphIdHasher idHasher = new GraphIdHasher();
        NodeDatabase database  = new NodeDatabase(DbConnection.getInstance(), factory);
        DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
        Metadata metadata = new Metadata(DbConnection.getInstance());

        Object object = new JColorChooser();
        ValueNode value = new ReflectionReader().read(object);
        FrameNode frame = new FrameNode();
        frame.setVariable("variable", value);

        time(() -> packer.pack(frame), "pack");
        AcyclicGraph dag = time(() -> AcyclicGraph.multiCondensationOf(frame), "dag");
        time(() -> hasher.assignHashes(dag), "hash");
        time(() -> idHasher.assignIdHashes(frame), "id-hash");

        dbIndex.createIndexes();
        time(() -> database.write(dag), "write");
        time(() -> metadata.addLocation(frame.hash(), new SourceLocation("Class", "method", 1)), "meta");
        database.read(frame.hash(), FrameNode.class);
    }

    private static void time(Runnable measured, String name) {
        long time = System.currentTimeMillis();
        measured.run();
        System.out.println(name + ": " + (System.currentTimeMillis() - time) + " ms");
    }

    private static <T> T time(Supplier<T> measured, String name) {
        long time = System.currentTimeMillis();
        T result = measured.get();
        System.out.println(name + ": " + (System.currentTimeMillis() - time) + " ms");
        return result;
    }
}
