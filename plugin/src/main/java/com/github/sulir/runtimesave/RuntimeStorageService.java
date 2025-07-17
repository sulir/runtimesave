package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.db.DbConnection;
import com.github.sulir.runtimesave.db.DbIndex;
import com.github.sulir.runtimesave.db.Metadata;
import com.github.sulir.runtimesave.db.HashedDb;
import com.github.sulir.runtimesave.graph.NodeFactory;
import com.github.sulir.runtimesave.hash.AcyclicGraph;
import com.github.sulir.runtimesave.hash.GraphHasher;
import com.github.sulir.runtimesave.hash.GraphIdHasher;
import com.github.sulir.runtimesave.hash.NodeHash;
import com.github.sulir.runtimesave.jdi.JdiReader;
import com.github.sulir.runtimesave.jdi.JdiWriter;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.pack.ValuePacker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.sun.jdi.StackFrame;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

@Service
public final class RuntimeStorageService {
    private final ExecutorService cpuPool = Executors.newWorkStealingPool();
    private final ExecutorService dbPool = Executors.newSingleThreadExecutor();
    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final NodeFactory factory = new NodeFactory(packer);
    private final ThreadLocal<GraphHasher> hasher = ThreadLocal.withInitial(GraphHasher::new);
    private final ThreadLocal<GraphIdHasher> idHasher = ThreadLocal.withInitial(GraphIdHasher::new);
    private final HashedDb database  = new HashedDb(DbConnection.getInstance(), factory);
    private final DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
    private boolean dbIndexed = false;
    private final Metadata metadata = new Metadata(DbConnection.getInstance());

    public static RuntimeStorageService getInstance() {
       return ApplicationManager.getApplication().getService(RuntimeStorageService.class);
    }

    public void loadFrame(StackFrame frame) throws MismatchException {
        NodeHash hash = metadata.findFrame(SourceLocation.fromJDI(frame.location()));
        FrameNode frameNode = database.read(hash, FrameNode.class);
        packer.unpack(frameNode);
        new JdiWriter(frame).writeFrame(frameNode);
    }

    public void saveFrame(StackFrame frame) {
        FrameNode frameNode = new JdiReader(frame).readFrame();
        SourceLocation location = SourceLocation.fromJDI(frame.location());

        cpuPool.execute(() -> {
            packer.pack(frameNode);
            AcyclicGraph dag = AcyclicGraph.multiCondensationOf(frameNode);
            hasher.get().assignHashes(dag);
            idHasher.get().assignIdHashes(frameNode);

            dbPool.execute(() -> {
                database.write(dag);
                metadata.addLocation(frameNode.hash(), location);
            });
        });
    }

    public void createIndexes(BooleanSupplier cancellationListener) {
        if (!dbIndexed) {
            dbIndex.setCancellationListener(cancellationListener);
            if (dbIndex.createIndexes())
                dbIndexed = true;
        }
    }
}
