package io.github.sulir.runtimesave.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.sun.jdi.StackFrame;
import io.github.sulir.runtimesave.db.DbConnection;
import io.github.sulir.runtimesave.db.DbIndex;
import io.github.sulir.runtimesave.db.HashedDb;
import io.github.sulir.runtimesave.db.Metadata;
import io.github.sulir.runtimesave.graph.NodeFactory;
import io.github.sulir.runtimesave.hash.AcyclicGraph;
import io.github.sulir.runtimesave.hash.GraphHasher;
import io.github.sulir.runtimesave.hash.GraphIdHasher;
import io.github.sulir.runtimesave.hash.NodeHash;
import io.github.sulir.runtimesave.jdi.JdiReader;
import io.github.sulir.runtimesave.jdi.JdiWriter;
import io.github.sulir.runtimesave.misc.BoundedExecutor;
import io.github.sulir.runtimesave.misc.MismatchException;
import io.github.sulir.runtimesave.misc.SourceLocation;
import io.github.sulir.runtimesave.nodes.FrameNode;
import io.github.sulir.runtimesave.pack.ValuePacker;

import java.util.function.BooleanSupplier;

@Service
public final class RuntimeStorageService {
    private final BoundedExecutor cpuPool = BoundedExecutor.usingAllCores();
    private final BoundedExecutor dbPool = BoundedExecutor.singleThreaded();
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
        NodeHash hash = metadata.findFrame(new JdiReader(frame).readLocation());
        FrameNode frameNode = database.read(hash, FrameNode.class);
        packer.unpack(frameNode);
        new JdiWriter(frame).writeFrame(frameNode);
    }

    public void saveFrame(StackFrame frame) {
        JdiReader reader = new JdiReader(frame);
        FrameNode frameNode = reader.readFrame();
        SourceLocation location = reader.readLocation();

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
