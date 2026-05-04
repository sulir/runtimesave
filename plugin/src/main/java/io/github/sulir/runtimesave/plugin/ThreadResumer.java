package io.github.sulir.runtimesave.plugin;

import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import io.github.sulir.runtimesave.config.RuntimeSaveSettings;
import io.github.sulir.runtimesave.misc.BoundedExecutor;
import io.github.sulir.runtimesave.sample.SamplingDebugExecutor;
import io.github.sulir.runtimesave.sample.SamplingRunExecutor;

import java.util.concurrent.TimeUnit;

public class ThreadResumer implements DebuggerManagerListener {
    @Override
    public void sessionAttached(DebuggerSession session) {
        XDebugSession xSession = session.getXDebugSession();
        if (xSession == null || !savepointOrSamplingEnabled(xSession))
            return;

        xSession.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                resumeOurThreads(session);
            }
        });
    }

    private boolean savepointOrSamplingEnabled(XDebugSession session) {
        UserDataHolderEx data = (UserDataHolderEx) session.getRunProfile();
        if (data != null && RuntimeSaveSettings.getOrDefault(data).isSavepointEnabled())
            return true;

        ExecutionEnvironment environment = session.getExecutionEnvironment();
        String id = (environment != null) ? environment.getExecutor().getId() : "";
        return id.equals(SamplingRunExecutor.EXECUTOR_ID) || id.equals(SamplingDebugExecutor.EXECUTOR_ID);
    }

    private void resumeOurThreads(DebuggerSession session) {
        if (!session.isPaused())
            return;

        VirtualMachine vm = session.getProcess().getVirtualMachineProxy().getVirtualMachine();
        for (ThreadReference thread : vm.allThreads()) {
            if (thread.name().startsWith(BoundedExecutor.PREFIX) || thread.name().startsWith("Neo4jDriverIO")) {
                int suspendCount = thread.suspendCount();
                for (int i = 0; i < suspendCount; i++)
                    thread.resume();
            }
        }

        scheduleNextThreadsResume(session);
    }

    private void scheduleNextThreadsResume(DebuggerSession session) {
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            DebuggerManagerThreadImpl managerThread = session.getContextManager().getContext().getManagerThread();
            if (managerThread == null)
                return;
            managerThread.schedule(new DebuggerCommandImpl() {
                @Override
                protected void action() {
                    resumeOurThreads(session);
                }
            });
        }, 500, TimeUnit.MILLISECONDS);
    }
}
