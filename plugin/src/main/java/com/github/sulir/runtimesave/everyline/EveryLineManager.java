package com.github.sulir.runtimesave.everyline;

import com.github.sulir.runtimesave.RuntimePersistenceService;
import com.github.sulir.runtimesave.SourceLocation;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

import java.util.HashMap;
import java.util.Map;

public class EveryLineManager {
    public static final int MAX_HITS = 1;

    private final ReferenceType clazz;
    private final Map<String, Integer> hitCounts = new HashMap<>();

    public EveryLineManager(ReferenceType clazz) {
        this.clazz = clazz;
    }

    public void addBreakpoints() {
        System.out.println("Adding breakpoints to " + clazz.name());

        try {
            for (Location location : clazz.allLineLocations()) {
                if (hitCounts.putIfAbsent(SourceLocation.fromJDI(location).toString(), 0) != null)
                    continue;

                EventRequestManager manager = clazz.virtualMachine().eventRequestManager();
                BreakpointRequest request = manager.createBreakpointRequest(location);

                DebugProcessEvents.enableRequestWithHandler(request,
                        (event) -> handleBreakpoint((BreakpointEvent) event));
            }
        } catch (AbsentInformationException ignored) {  }
    }

    private void handleBreakpoint(BreakpointEvent event) {
        String classLine = SourceLocation.fromJDI(event.location()).toString();
        int newHitCount = hitCounts.get(classLine) + 1;
        hitCounts.put(classLine, newHitCount);

        if (newHitCount >= MAX_HITS)
            event.virtualMachine().eventRequestManager().deleteEventRequest(event.request());

        try {
            System.out.println(event.location().sourcePath() + ":" + event.location().lineNumber());
            StackFrame frame = event.thread().frame(0);
            RuntimePersistenceService.getInstance().saveFrame(frame);
        } catch (AbsentInformationException | IncompatibleThreadStateException e) {
            throw new RuntimeException(e);
        }
    }
}
