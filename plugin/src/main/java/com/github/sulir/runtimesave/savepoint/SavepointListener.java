package com.github.sulir.runtimesave.savepoint;

import com.github.sulir.runtimesave.RuntimeStorageService;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.Alarm;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.sun.jdi.StackFrame;

import java.util.function.Consumer;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class SavepointListener implements DebuggerManagerListener {
    public static final int DEBUG_WINDOW_DISABLED_MS = 1000;

    private final Project project;
    private final Alarm reEnableWindowSchedule = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private boolean windowWasVisible = false;

    public SavepointListener(Project project) {
        this.project = project;
    }

    @Override
    public void sessionAttached(DebuggerSession session) {
        if (session.getXDebugSession() == null)
            return;

        session.getXDebugSession().addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                if (isPausedAtSavepoint(session)) {
                    disableDebugWindowTemporarily();
                    StackFrameProxy proxy = session.getContextManager().getContext().getFrameProxy();
                    if (proxy == null)
                        return;

                    StackFrame frame = uncheck(proxy::getStackFrame);
                    RuntimeStorageService.getInstance().saveFrame(frame);

                    ApplicationManager.getApplication().invokeLater(session::resume);
                } else {
                    if (!reEnableWindowSchedule.isEmpty())
                        reEnableDebugWindow();
                }
            }

            @Override
            public void sessionStopped() {
                if (!reEnableWindowSchedule.isEmpty())
                    reEnableDebugWindow();
            }
        });
    }

    private boolean isPausedAtSavepoint(DebuggerSession session) {
        SavepointType savepointType = SavepointType.getInstance();

        PsiElement element = ApplicationManager.getApplication().runReadAction((Computable<PsiElement>) () ->
                session.getContextManager().getContext().getContextElement());
        if (element == null)
            return false;

        VirtualFile file = ApplicationManager.getApplication().runReadAction((Computable<VirtualFile>) () ->
             element.getContainingFile().getVirtualFile());
        if (file == null)
            return false;

        Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () ->
                FileDocumentManager.getInstance().getDocument(file));
        if (document == null)
            return false;
        int line = document.getLineNumber(element.getTextOffset());

        XBreakpointManager manager = XDebuggerManager.getInstance(session.getProject()).getBreakpointManager();
        manager.findBreakpointsAtLine(savepointType, file, line);

        return manager.findBreakpointsAtLine(savepointType, file, line).stream().anyMatch(b ->
                b.getType() instanceof SavepointType);
    }

    private void disableDebugWindowTemporarily() {
        doWithDebugToolWindow((window) -> {
            if (reEnableWindowSchedule.isEmpty())
                windowWasVisible = window.isVisible();

            window.setAvailable(false);
            reEnableWindowSchedule.cancelAllRequests();
            reEnableWindowSchedule.addRequest(this::reEnableDebugWindow, DEBUG_WINDOW_DISABLED_MS);
        });
    }

    private void reEnableDebugWindow() {
        doWithDebugToolWindow((window) -> {
            window.setAvailable(true);
            if (windowWasVisible)
                window.show();
        });
    }

    private void doWithDebugToolWindow(Consumer<ToolWindow> action) {
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        manager.invokeLater(() -> {
            ToolWindow window = manager.getToolWindow(ToolWindowId.DEBUG);
            if (window != null)
                action.accept(window);
        });
    }
}
