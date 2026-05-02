package io.github.sulir.runtimesave.savepoint;

import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import io.github.sulir.runtimesave.config.RuntimeSaveSettings;
import io.github.sulir.runtimesave.misc.BoundedExecutor;

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
        VirtualMachine vm = session.getProcess().getVirtualMachineProxy().getVirtualMachine();
        for (ThreadReference thread : vm.allThreads()) {
            if (thread.name().startsWith(BoundedExecutor.PREFIX) || thread.name().startsWith("Neo4jDriverIO")) {
                int suspendCount = thread.suspendCount();
                for (int i = 0; i < suspendCount; i++)
                    thread.resume();
            }
        }
    }
}
