package com.github.sulir.runtimesave.sample;

import com.github.sulir.runtimesave.config.SamplingSettings;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessEvents;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.xdebugger.XDebugSession;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import org.jetbrains.annotations.NotNull;

public class JdiSamplingListener implements DebuggerManagerListener {
    private static final String[] EXCLUDED = {
            "com.sun.*", "java.*", "javax.*", "jdk.*", "sun.*",
            "com.intellij.execution.*", "com.intellij.rt.*", "org.jetbrains.capture.*",
            "com.github.sulir.runtimesave.*"
    };

    @Override
    public void sessionCreated(DebuggerSession session) {
        session.getProcess().addDebugProcessListener(new DebugProcessListener() {
            @Override
            public void processAttached(@NotNull DebugProcess process) {
                XDebugSession xSession;
                ExecutionEnvironment environment;
                if ((xSession = session.getXDebugSession()) == null
                        || (environment = xSession.getExecutionEnvironment()) == null
                        || environment.getExecutionId() != SamplingDebugRunner.UID)
                    return;

                VirtualMachine vm = ((VirtualMachineProxyImpl) process.getVirtualMachineProxy()).getVirtualMachine();
                RunConfigurationBase<?> profile = (RunConfigurationBase<?>) environment.getRunProfile();
                SamplingSettings settings = profile.getUserData(SamplingSettings.key);
                setClassPrepareRequest(vm, settings);
            }
        });
    }

    private void setClassPrepareRequest(VirtualMachine vm, SamplingSettings settings) {
        EventRequestManager manager = vm.eventRequestManager();
        ClassPrepareRequest request = manager.createClassPrepareRequest();

        for (String included : settings.getIncludeClassPatterns())
            request.addClassFilter(included);
        for (String excluded : EXCLUDED)
            request.addClassExclusionFilter(excluded);
        int everyNthLine = settings.getEveryNthLine();
        int firstTExecutions = settings.getFirstTExecutions();

        DebugProcessEvents.enableRequestWithHandler(request, (ev) -> {
            ClassPrepareEvent event = (ClassPrepareEvent) ev;
            new JdiSamplingManager(everyNthLine, firstTExecutions).addBreakpoints(event.referenceType());
        });
    }
}
