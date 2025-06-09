package com.github.sulir.runtimesave.start;

import com.github.sulir.runtimesave.db.Database;
import com.github.sulir.runtimesave.graph.*;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebugSession;
import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StartLineListener implements DebuggerManagerListener {
    @Override
    public void sessionCreated(DebuggerSession session) {
        session.getProcess().addDebugProcessListener(new DebugProcessListener() {
            @Override
            public void processAttached(@NotNull DebugProcess process) {
                XDebugSession xSession = session.getXDebugSession();
                if (xSession == null || !(xSession.getRunProfile() instanceof ApplicationConfiguration configuration))
                    return;
                if (!StartProgramAction.MAIN_CLASS.equals(configuration.getMainClassName()))
                    return;

                if (configuration.getProgramParameters() == null)
                    return;
                String[] params = configuration.getProgramParameters().split(" ");
                if (params.length < 4)
                    return;

                String className = params[0];
                int line = Integer.parseInt(params[3]);

                VirtualMachine vm = ((VirtualMachineProxyImpl) process.getVirtualMachineProxy()).getVirtualMachine();
                addBreakpoint(vm, className, line);
            }
        });
    }

    private void addBreakpoint(VirtualMachine vm, String className, int line) {
        ClassPrepareRequest request = vm.eventRequestManager().createClassPrepareRequest();
        request.addClassFilter(className);

        DebugProcessEvents.enableRequestWithHandler(request, (ev) -> {
            vm.eventRequestManager().deleteEventRequest(ev.request());
            ClassPrepareEvent classPrepareEvent = (ClassPrepareEvent) ev;
            ReferenceType clazz = classPrepareEvent.referenceType();

            try {
                List<Location> locations = clazz.locationsOfLine(line);
                if (locations.isEmpty()) {
                    stopProgram(vm, "Cannot start program at line " + line
                            + ". It probably does not contain executable code.");
                    return;
                }

                BreakpointRequest breakpoint = vm.eventRequestManager().createBreakpointRequest(locations.get(0));
                DebugProcessEvents.enableRequestWithHandler(breakpoint, this::handleBreakpoint);
            } catch (AbsentInformationException e) {
                stopProgram(vm, "Line number information absent for class " + className);
            }
        });
    }

    private void handleBreakpoint(Event event) {
        event.virtualMachine().eventRequestManager().deleteEventRequest(event.request());
        BreakpointEvent breakpointEvent = (BreakpointEvent) event;

        try {
            StackFrame frame = breakpointEvent.thread().frame(0);

            DbMetadata search = new DbMetadata(Database.getInstance());
            String frameId = search.findFrame(SourceLocation.fromJDI(frame.location()));
            DbReader reader = new DbReader(Database.getInstance());
            FrameNode frameNode = reader.readFrame(frameId);
            JdiWriter writer = new JdiWriter(frame);
            writer.writeFrame(frameNode);
        } catch (IncompatibleThreadStateException | MismatchException e) {
            stopProgram(event.virtualMachine(), e.getMessage());
        }
    }

    private void stopProgram(VirtualMachine vm, String message) {
        ApplicationManager.getApplication().invokeLater(() ->
                Messages.showErrorDialog(message, "Error")
        );
        vm.exit(1);
    }
}
