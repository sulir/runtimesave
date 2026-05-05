package io.github.sulir.runtimesave.savepoint;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import io.github.sulir.runtimesave.config.RuntimeSaveSettings;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@Service(Service.Level.PROJECT)
public final class InvalidSavepointService implements Disposable {
    public InvalidSavepointService(Project project) {
        project.getMessageBus().connect(this).subscribe(XBreakpointListener.TOPIC, new XBreakpointListener<>() {
            @Override
            public void breakpointPresentationUpdated(@NonNull XBreakpoint breakpoint, @Nullable XDebugSession session) {
                makeSavepointInvalidIfDisabled(breakpoint, session);
            }
        });
    }

    private void makeSavepointInvalidIfDisabled(XBreakpoint<?> breakpoint, XDebugSession session) {
        if (!(breakpoint instanceof XLineBreakpoint<?> lineBP) || !(breakpoint.getType() instanceof SavepointType))
            return;
        if (session == null || !(session.getRunProfile() instanceof UserDataHolderEx data))
            return;

        if (!RuntimeSaveSettings.getOrDefault(data).isSavepointEnabled())
            session.setBreakpointInvalid(lineBP, "Savepoints are disabled in Run/Debug Configuration");
    }

    @Override
    public void dispose() { }
}
