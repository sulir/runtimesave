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
        packer.pack(frame);
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(frame);
        hasher.assignHashes(dag);
        idHasher.assignIdHashes(frame);
        dbIndex.createIndexes();

        long time = System.currentTimeMillis();
        database.write(dag);
        metadata.addLocation(frame.hash(), new SourceLocation("Class", "method", 1));
        System.out.println("Written in " + (System.currentTimeMillis() - time) + " ms");
        database.read(frame.hash(), FrameNode.class);
    }
}
