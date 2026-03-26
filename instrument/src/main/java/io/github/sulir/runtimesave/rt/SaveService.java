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
import io.github.sulir.runtimesave.misc.SourceLocation;
import io.github.sulir.runtimesave.nodes.FrameNode;
import io.github.sulir.runtimesave.pack.ValuePacker;

public class SaveService {
    private static SaveService instance;

    private final BoundedExecutor thread = BoundedExecutor.singleThreaded();
    private final DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final Metadata metadata = new Metadata(DbConnection.getInstance());
    private final GraphHasher hasher = new GraphHasher();
    private final GraphIdHasher idHasher = new GraphIdHasher();
    private final NodeFactory factory = new NodeFactory(packer);
    private final HashedDb database  = new HashedDb(DbConnection.getInstance(), factory);

    public static SaveService getInstance() {
        if (instance == null)
            instance = new SaveService();
        return instance;
    }

    public void createIndexes() {
        dbIndex.createIndexes();
    }

    public void saveLocation(SourceLocation location) {
        thread.execute(() -> {
            FrameNode frame = new FrameNode();
            packer.pack(frame);
            AcyclicGraph dag = AcyclicGraph.multiCondensationOf(frame);
            hasher.assignHashes(dag);
            idHasher.assignIdHashes(frame);
            database.write(dag);
            metadata.addLocation(frame.hash(), location);
        });
    }
}
