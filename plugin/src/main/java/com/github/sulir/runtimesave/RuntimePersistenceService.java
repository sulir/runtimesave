package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.db.DbConnection;
import com.github.sulir.runtimesave.db.Metadata;
import com.github.sulir.runtimesave.db.NodeDatabase;
import com.github.sulir.runtimesave.jdi.JdiReader;
import com.github.sulir.runtimesave.jdi.JdiWriter;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.sun.jdi.StackFrame;

@Service
public final class RuntimePersistenceService {
    private final NodeDatabase database;
    private final Metadata metadata;

    public static RuntimePersistenceService getInstance() {
       return ApplicationManager.getApplication().getService(RuntimePersistenceService.class);
    }

    public RuntimePersistenceService() {
        database = new NodeDatabase(DbConnection.getInstance());
        metadata = new Metadata(DbConnection.getInstance());
    }

    public void loadFrame(StackFrame frame) throws MismatchException {
        String frameId = metadata.findFrame(SourceLocation.fromJDI(frame.location()));
        FrameNode frameNode = database.read(frameId, FrameNode.class);
        new JdiWriter(frame).writeFrame(frameNode);
    }

    public void saveFrame(StackFrame frame) {
        FrameNode frameNode = new JdiReader(frame).readFrame();
        String frameId = database.write(frameNode);
        metadata.addLocation(frameId, SourceLocation.fromJDI(frame.location()));
    }
}
