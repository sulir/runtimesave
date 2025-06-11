package com.github.sulir.runtimesave.savepoints;

import com.github.sulir.runtimesave.RuntimePersistenceService;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.impl.DebuggerManagerListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.sun.jdi.StackFrame;

public class SavepointListener implements DebuggerManagerListener {
    @Override
    public void sessionAttached(DebuggerSession session) {
        if (session.getXDebugSession() == null)
            return;

        session.getXDebugSession().addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                if (isPausedAtSavepoint(session)) {
                    try {
                        StackFrameProxy proxy = session.getContextManager().getContext().getFrameProxy();
                        if (proxy == null)
                            return;

                        StackFrame frame = proxy.getStackFrame();
                        RuntimePersistenceService.getInstance().saveFrame(frame);

                        ApplicationManager.getApplication().invokeLater(session::resume);
                    } catch (EvaluateException e) {
                        throw new RuntimeException(e);
                    }
                }
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
}
