package com.github.sulir.runtimesave.everyline;

import com.github.sulir.runtimesave.StatePersistence;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

import java.util.HashMap;
import java.util.Map;

public class BreakpointManager {
    public static final int MAX_HITS = 1;

    private final ReferenceType clazz;
    private final Map<Location, Integer> hitCounts = new HashMap<>();

    public BreakpointManager(ReferenceType clazz) {
        this.clazz = clazz;
    }

    public void addToEveryLine() {
        System.out.println("Adding breakpoints to " + clazz.name());

        try {
            for (Location location : clazz.allLineLocations()) {
                EventRequestManager manager = clazz.virtualMachine().eventRequestManager();
                BreakpointRequest request = manager.createBreakpointRequest(location);

                DebugProcessEvents.enableRequestWithHandler(request, (event) -> {
                    handleBreakpoint((BreakpointEvent) event);
                });
            }
        } catch (AbsentInformationException ignored) {  }
    }

    private void handleBreakpoint(BreakpointEvent event) {
        int newHitCount = hitCounts.getOrDefault(event.location(), 0) + 1;
        hitCounts.put(event.location(), newHitCount);

        if (newHitCount >= MAX_HITS)
            event.virtualMachine().eventRequestManager().deleteEventRequest(event.request());

        try {
            System.out.println(event.location().sourcePath() + ":" + event.location().lineNumber());
        } catch (AbsentInformationException e) {
            throw new RuntimeException(e);
        }

        try {
            StatePersistence persistence = new StatePersistence(event.thread().frame(0));
            persistence.saveThisAndLocals();
        } catch (IncompatibleThreadStateException e) {
            throw new RuntimeException(e);
        }
    }
}
