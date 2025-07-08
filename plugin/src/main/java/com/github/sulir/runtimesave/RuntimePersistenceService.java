package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.db.DbConnection;
import com.github.sulir.runtimesave.db.Metadata;
import com.github.sulir.runtimesave.db.NodeDatabase;
import com.github.sulir.runtimesave.graph.NodeFactory;
import com.github.sulir.runtimesave.hash.AcyclicGraph;
import com.github.sulir.runtimesave.hash.GraphHasher;
import com.github.sulir.runtimesave.hash.GraphIdHasher;
import com.github.sulir.runtimesave.hash.NodeHash;
import com.github.sulir.runtimesave.jdi.JdiReader;
import com.github.sulir.runtimesave.jdi.JdiWriter;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.packers.ValuePacker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.sun.jdi.StackFrame;

@Service
public final class RuntimePersistenceService {
    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final NodeFactory factory = new NodeFactory(packer);
    private final GraphHasher hasher = new GraphHasher();
    private final GraphIdHasher idHasher = new GraphIdHasher();
    private final NodeDatabase database  = new NodeDatabase(DbConnection.getInstance(), factory);
    private final Metadata metadata = new Metadata(DbConnection.getInstance());

    public static RuntimePersistenceService getInstance() {
       return ApplicationManager.getApplication().getService(RuntimePersistenceService.class);
    }

    public void loadFrame(StackFrame frame) throws MismatchException {
        NodeHash hash = metadata.findFrame(SourceLocation.fromJDI(frame.location()));
        FrameNode frameNode = database.read(hash, FrameNode.class);
        packer.unpack(frameNode);
        new JdiWriter(frame).writeFrame(frameNode);
    }

    public void saveFrame(StackFrame frame) {
        FrameNode frameNode = new JdiReader(frame).readFrame();
        packer.pack(frameNode);
        AcyclicGraph dag = AcyclicGraph.multiCondensationOf(frameNode);
        hasher.assignHashes(dag);
        idHasher.assignIdHashes(frameNode);
        database.write(dag);
        metadata.addLocation(frameNode.hash(), SourceLocation.fromJDI(frame.location()));
    }
}
