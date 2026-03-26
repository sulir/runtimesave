package io.github.sulir.runtimesave.sample;

import com.intellij.debugger.engine.DebugProcessEvents;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import io.github.sulir.runtimesave.misc.SourceLocation;
import io.github.sulir.runtimesave.plugin.RuntimeStorageService;

import java.util.HashMap;
import java.util.Map;

import static io.github.sulir.runtimesave.misc.UncheckedThrowing.uncheck;

public class JdiSamplingManager {
    private static final boolean DEBUG = System.getenv("RS_DEBUG") != null;

    private final int everyNthLine;
    private final int firstTExecutions;
    private int linesHit = 0;
    private final Map<SourceLocation, Integer> hitsPerLine = new HashMap<>();

    public JdiSamplingManager(int everyNthLine, int firstTExecutions) {
        this.everyNthLine = everyNthLine;
        this.firstTExecutions = firstTExecutions;
    }

    public void addBreakpoints(ReferenceType clazz) {
        if (DEBUG)
            System.out.println("Adding breakpoints to " + clazz.name());
        if (firstTExecutions == 0)
            return;

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

        if (newHitCount >= firstTExecutions && firstTExecutions != -1)
            event.virtualMachine().eventRequestManager().deleteEventRequest(event.request());

        if (DEBUG)
            System.out.println(uncheck(event.location()::sourcePath) + ":" + event.location().lineNumber());
        StackFrame frame = uncheck(() -> event.thread().frame(0));
        RuntimeStorageService.getInstance().saveFrame(frame);
    }
}
