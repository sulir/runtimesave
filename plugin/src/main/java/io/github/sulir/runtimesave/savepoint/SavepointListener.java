package io.github.sulir.runtimesave.savepoint;

import com.intellij.debugger.engine.DebuggerManagerThreadImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import io.github.sulir.runtimesave.config.RuntimeSaveSettings;
import io.github.sulir.runtimesave.misc.BoundedExecutor;

import java.util.concurrent.TimeUnit;

public class SavepointListener implements DebuggerManagerListener {
    @Override
    public void sessionAttached(DebuggerSession session) {
        XDebugSession xSession = session.getXDebugSession();
        if (xSession == null)
            return;

        xSession.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                UserDataHolderEx data = (UserDataHolderEx) xSession.getRunProfile();
                if (data == null || !RuntimeSaveSettings.getOrDefault(data).isSavepointEnabled())
                    return;

                resumeOurThreads(session);
            }
        });
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
