package io.github.sulir.runtimesave.rt;

import io.github.sulir.runtimesave.db.DbConnection;
import io.github.sulir.runtimesave.db.DbIndex;
import io.github.sulir.runtimesave.db.HashedDb;
import io.github.sulir.runtimesave.db.Metadata;
import io.github.sulir.runtimesave.graph.NodeFactory;
import io.github.sulir.runtimesave.hash.AcyclicGraph;
import io.github.sulir.runtimesave.hash.GraphHasher;
import io.github.sulir.runtimesave.hash.GraphIdHasher;
import io.github.sulir.runtimesave.misc.BoundedExecutor;
import io.github.sulir.runtimesave.misc.Log;
import io.github.sulir.runtimesave.misc.SourceLocation;
import io.github.sulir.runtimesave.nodes.FrameNode;
import io.github.sulir.runtimesave.pack.ValuePacker;

public class SaveService {
    private static SaveService instance;

    private final BoundedExecutor cpuPool = BoundedExecutor.usingAllCores();
    private final BoundedExecutor dbPool = BoundedExecutor.singleThreaded();
    private final DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final ThreadLocal<GraphHasher> hasher = ThreadLocal.withInitial(GraphHasher::new);
    private final ThreadLocal<GraphIdHasher> idHasher = ThreadLocal.withInitial(GraphIdHasher::new);
    private final NodeFactory factory = new NodeFactory(packer);
    private final HashedDb database  = new HashedDb(DbConnection.getInstance(), factory);
    private final Metadata metadata = new Metadata(DbConnection.getInstance());

    public static SaveService getInstance() {
        if (instance == null)
            instance = new SaveService();
        return instance;
    }

    public void createIndexes() {
        dbIndex.createIndexes();
    }

    public void saveFrame(BufferReader reader) {
        reader.readClasses();

        cpuPool.execute(() -> {
            SourceLocation location;
            FrameNode frame;
            try (reader) {
                location = reader.readLocation();
                frame = reader.readFrame();
            }
            Log.info(location, frame);

            if ("no".equals(System.getenv("RS_WRITE")))
                return;

            packer.pack(frame);
            AcyclicGraph dag = AcyclicGraph.multiCondensationOf(frame);
            hasher.get().assignHashes(dag);
            idHasher.get().assignIdHashes(frame);

            dbPool.execute(() -> {
                database.write(dag);
                metadata.addLocation(frame.hash(), location);
            });
        });
    }
}
