package com.github.sulir.runtimesave.everyline;

import com.github.sulir.runtimesave.graph.JdiReader;
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

public class EveryLineManager {
    public static final int MAX_HITS = 1;

    private final ReferenceType clazz;
    private final Map<Location, Integer> hitCounts = new HashMap<>();

    public EveryLineManager(ReferenceType clazz) {
        this.clazz = clazz;
    }

    public void addBreakpoints() {
        System.out.println("Adding breakpoints to " + clazz.name());

        try {
            for (Location location : clazz.allLineLocations()) {
                EventRequestManager manager = clazz.virtualMachine().eventRequestManager();
                BreakpointRequest request = manager.createBreakpointRequest(location);

                DebugProcessEvents.enableRequestWithHandler(request,
                        (event) -> handleBreakpoint((BreakpointEvent) event));
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
            JdiReader saver = new JdiReader(event.thread().frame(0));
            saver.saveThisAndLocals();
        } catch (IncompatibleThreadStateException e) {
            throw new RuntimeException(e);
        }
    }
}
