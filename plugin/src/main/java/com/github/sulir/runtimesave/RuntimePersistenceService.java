package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.db.Database;
import com.github.sulir.runtimesave.db.DbMetadata;
import com.github.sulir.runtimesave.db.DbReader;
import com.github.sulir.runtimesave.db.DbWriter;
import com.github.sulir.runtimesave.jdi.JdiReader;
import com.github.sulir.runtimesave.jdi.JdiWriter;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.sun.jdi.StackFrame;

@Service
public final class RuntimePersistenceService {
    private final DbReader dbReader;
    private final DbWriter dbWriter;
    private final DbMetadata dbMetadata;

    public static RuntimePersistenceService getInstance() {
       return ApplicationManager.getApplication().getService(RuntimePersistenceService.class);
    }

    public RuntimePersistenceService() {
        Database database = Database.getInstance();
        dbReader = new DbReader(database);
        dbWriter = new DbWriter(database);
        dbMetadata = new DbMetadata(database);
    }

    public void loadFrame(StackFrame frame) throws MismatchException {
        String frameId = dbMetadata.findFrame(SourceLocation.fromJDI(frame.location()));
        FrameNode frameNode = dbReader.read(frameId, FrameNode.class);
        new JdiWriter(frame).writeFrame(frameNode);
    }

    public void saveFrame(StackFrame frame) {
        FrameNode frameNode = new JdiReader(frame).readFrame();
        String frameId = dbWriter.writeNode(frameNode);
        dbMetadata.addLocation(frameId, SourceLocation.fromJDI(frame.location()));
    }
}
