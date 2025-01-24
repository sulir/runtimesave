package com.github.sulir.runtimesave.everyline;

import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import org.jetbrains.annotations.NotNull;

public class EveryLineListener implements DebuggerManagerListener {
    @Override
    public void sessionCreated(DebuggerSession session) {
        session.getProcess().addDebugProcessListener(new DebugProcessListener() {
            @Override
            public void processAttached(@NotNull DebugProcess process) {
                VirtualMachine vm = ((VirtualMachineProxyImpl) process.getVirtualMachineProxy()).getVirtualMachine();
                setClassPrepareRequest(vm, session.getProject());
            }
        });
    }

    private void setClassPrepareRequest(VirtualMachine vm, Project project) {
        EventRequestManager manager = vm.eventRequestManager();
        ClassPrepareRequest request = manager.createClassPrepareRequest();
        request.addClassFilter("org.apache.commons.lang3.*");

        DebugProcessEvents.enableRequestWithHandler(request, (ev) -> {
            ClassPrepareEvent event = (ClassPrepareEvent) ev;

            if (isProjectClass(event.referenceType(), project))
                new EveryLineManager(event.referenceType()).addToEveryLine();
        });
    }

    private boolean isProjectClass(ReferenceType type, Project project) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        int innerClassIndex = type.name().indexOf('$');
        String baseName = (innerClassIndex == -1) ? type.name() : type.name().substring(0, innerClassIndex);
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () ->
                JavaPsiFacade.getInstance(project).findClass(baseName, scope) != null);
    }
}
