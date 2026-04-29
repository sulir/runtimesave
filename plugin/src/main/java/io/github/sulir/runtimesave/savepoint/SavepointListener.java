package io.github.sulir.runtimesave.savepoint;

import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.Alarm;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.sun.jdi.ClassType;
import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import io.github.sulir.runtimesave.config.RuntimeSaveSettings;
import io.github.sulir.runtimesave.misc.BoundedExecutor;

import java.util.List;
import java.util.function.Consumer;

import static io.github.sulir.runtimesave.misc.UncheckedThrowing.uncheck;

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
                UserDataHolderEx data = (UserDataHolderEx) session.getXDebugSession().getRunProfile();
                if (data == null || !RuntimeSaveSettings.getOrDefault(data).isSavepointEnabled())
                    return;

                if (isPausedAtSavepoint(session)) {
                    disableDebugWindowTemporarily();
                    startDataCollection(session);
                    ApplicationManager.getApplication().invokeLater(session::resume);
                } else {
                    if (!reEnableWindowSchedule.isEmpty())
                        reEnableDebugWindow();
                    resumeOurThreads(session);
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

    private void startDataCollection(DebuggerSession session) {
        ThreadReferenceProxy proxy = session.getContextManager().getContext().getThreadProxy();
        if (proxy == null)
            return;
        ThreadReference thread = proxy.getThreadReference();

        ClassType collector = (ClassType) thread.virtualMachine()
                .classesByName("io.github.sulir.runtimesave.rt.Collector").getFirst();
        Method method = collector.concreteMethodByName("collectAlways", "()V");
        uncheck(() -> collector.invokeMethod(thread, method, List.of(), 0));
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
