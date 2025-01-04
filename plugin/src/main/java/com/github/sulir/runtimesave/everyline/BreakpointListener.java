package com.github.sulir.runtimesave.everyline;

import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import org.jetbrains.annotations.NotNull;

public class BreakpointListener implements DebuggerManagerListener {
    @Override
    public void sessionCreated(DebuggerSession session) {
        session.getProcess().addDebugProcessListener(new DebugProcessListener() {
            @Override
            public void processAttached(@NotNull DebugProcess process) {
                VirtualMachine vm = ((VirtualMachineProxyImpl) process.getVirtualMachineProxy()).getVirtualMachine();
                setClassPrepareRequest(vm);
            }
        });
    }

    private void setClassPrepareRequest(VirtualMachine vm) {
        EventRequestManager manager = vm.eventRequestManager();
        ClassPrepareRequest request = manager.createClassPrepareRequest();
        request.addClassFilter("org.apache.commons.lang3.*");

        DebugProcessEvents.enableRequestWithHandler(request, (ev) -> {
            ClassPrepareEvent event = (ClassPrepareEvent) ev;
            new BreakpointManager(event.referenceType()).addToEveryLine();
        });
    }
}
