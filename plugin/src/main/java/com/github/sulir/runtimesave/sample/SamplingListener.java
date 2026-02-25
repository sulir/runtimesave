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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebugSession;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import org.jetbrains.annotations.NotNull;

public class SamplingListener implements DebuggerManagerListener {
    private static final String[] EXCLUDED = {"com.sun.*", "java.*", "javax.*", "jdk.*", "sun.*"};

    private final String[] included;

    public SamplingListener() {
        String includedProperty = System.getenv("RS_INCLUDE");
        included = includedProperty == null ? new String[0] : includedProperty.split(",");
    }

    @Override
    public void sessionCreated(DebuggerSession session) {
        session.getProcess().addDebugProcessListener(new DebugProcessListener() {
            @Override
            public void processAttached(@NotNull DebugProcess process) {
                XDebugSession xSession;
                ExecutionEnvironment environment;
                if ((xSession = session.getXDebugSession()) == null
                        || (environment = xSession.getExecutionEnvironment()) == null
                        || System.getenv("RS_INSTRUMENT") != null)
                    return;

                VirtualMachine vm = ((VirtualMachineProxyImpl) process.getVirtualMachineProxy()).getVirtualMachine();
                RunConfigurationBase<?> profile = (RunConfigurationBase<?>) environment.getRunProfile();
                SamplingSettings settings = profile.getUserData(SamplingSettings.key);
                setClassPrepareRequest(vm, session.getProject(), settings);
            }
        });
    }

    private void setClassPrepareRequest(VirtualMachine vm, Project project, SamplingSettings settings) {
        EventRequestManager manager = vm.eventRequestManager();
        ClassPrepareRequest request = manager.createClassPrepareRequest();

        for (String pattern : included)
            request.addClassFilter(pattern);
        for (String pattern : EXCLUDED)
            request.addClassExclusionFilter(pattern);
        int everyNthLine = settings.getEveryNthLine();
        int firstTExecutions = settings.getFirstTExecutions();

        DebugProcessEvents.enableRequestWithHandler(request, (ev) -> {
            ClassPrepareEvent event = (ClassPrepareEvent) ev;

            if (included.length != 0 || isProjectClass(event.referenceType(), project))
                new SamplingManager(everyNthLine, firstTExecutions).addBreakpoints(event.referenceType());
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
