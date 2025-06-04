package com.github.sulir.runtimesave.app;

import com.github.sulir.runtimesave.db.DBSaver;
import com.github.sulir.runtimesave.db.SourceLocation;
import com.github.sulir.runtimesave.graph.GraphNode;
import com.github.sulir.runtimesave.graph.GraphSaver;

public class SaveHelper {
    public static void ensureLoadedForJdi() { }

    public static void savePrimitive(String name, int value) {
        System.out.println("Saving primitive: " + name + " = " + value);
        // saveBoxed(name, value); // hangs when called from JDI, even on the 1st line
    }

    private static void saveBoxed(String name, Object value) {
        GraphSaver graphSaver = new GraphSaver(value, true);
        GraphNode node = graphSaver.save();
        DBSaver dbSaver = new DBSaver(node);
        dbSaver.save();
        SourceLocation location = SourceLocation.fromStackTrace(-3);
        dbSaver.addMetadata(location, name);
    }
}
