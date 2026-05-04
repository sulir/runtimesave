package io.github.sulir.runtimesave.start;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import io.github.sulir.runtimesave.db.DbConnection;
import io.github.sulir.runtimesave.db.HashedDb;
import io.github.sulir.runtimesave.db.Metadata;
import io.github.sulir.runtimesave.graph.NodeFactory;
import io.github.sulir.runtimesave.hash.NodeHash;
import io.github.sulir.runtimesave.misc.MismatchException;
import io.github.sulir.runtimesave.misc.SourceLocation;
import io.github.sulir.runtimesave.nodes.FrameNode;
import io.github.sulir.runtimesave.pack.ValuePacker;

@Service
public final class LoadService {
    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final NodeFactory factory = new NodeFactory(packer);
    private final HashedDb database  = new HashedDb(DbConnection.getInstance(), factory);
    private final Metadata metadata = new Metadata(DbConnection.getInstance());

    public static LoadService getInstance() {
       return ApplicationManager.getApplication().getService(LoadService.class);
    }

    public void loadFrame(StackFrame frame) throws MismatchException {
        NodeHash hash = metadata.findFrame(readLocation(frame));
        FrameNode frameNode = database.read(hash, FrameNode.class);
        packer.unpack(frameNode);
        new JdiWriter(frame).writeFrame(frameNode);
    }

    private SourceLocation readLocation(StackFrame frame) {
        Location location = frame.location();
        String className = location.declaringType().name();
        String method = location.method().name() + location.method().signature();
        return new SourceLocation(className, method, location.lineNumber());
    }
}
