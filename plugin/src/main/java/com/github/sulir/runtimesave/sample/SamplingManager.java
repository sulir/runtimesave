package com.github.sulir.runtimesave.sample;

import com.github.sulir.runtimesave.RuntimeStorageService;
import com.github.sulir.runtimesave.SourceLocation;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;

import java.util.HashMap;
import java.util.Map;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class SamplingManager {
    private final int everyNthLine;
    private final int nPerLine;
    private int linesHit = 0;
    private final Map<SourceLocation, Integer> hitsPerLine = new HashMap<>();

    public SamplingManager(int everyNthLine, int nPerLine) {
        this.everyNthLine = everyNthLine;
        this.nPerLine = nPerLine;
    }

    public void addBreakpoints(ReferenceType clazz) {
        System.out.println("Adding breakpoints to " + clazz.name());

        try {
            for (Location location : clazz.allLineLocations()) {
                linesHit = (linesHit + 1) % everyNthLine;
                if (linesHit != 0)
                    continue;

                if (hitsPerLine.putIfAbsent(SourceLocation.fromJDI(location), 0) != null)
                    continue;

                EventRequestManager manager = clazz.virtualMachine().eventRequestManager();
                BreakpointRequest request = manager.createBreakpointRequest(location);

                DebugProcessEvents.enableRequestWithHandler(request,
                        (event) -> handleBreakpoint((BreakpointEvent) event));
            }
        } catch (AbsentInformationException ignored) {  }
    }

    private void handleBreakpoint(BreakpointEvent event) {
        SourceLocation location = SourceLocation.fromJDI(event.location());
        int newHitCount = hitsPerLine.get(location) + 1;
        hitsPerLine.put(location, newHitCount);

        if (newHitCount >= nPerLine)
            event.virtualMachine().eventRequestManager().deleteEventRequest(event.request());

        System.out.println(uncheck(event.location()::sourcePath) + ":" + event.location().lineNumber());
        StackFrame frame = uncheck(() -> event.thread().frame(0));
        RuntimeStorageService.getInstance().saveFrame(frame);
    }
}
