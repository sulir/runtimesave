package com.github.sulir.runtimesave.savepoints;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.impl.actions.handlers.AddLineBreakpointAction;
import org.jetbrains.annotations.NotNull;

public class AddSavepointAction extends AddLineBreakpointAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || file == null || editor == null)
            return;

        SavepointType savepointType = SavepointType.getInstance();
        EditorGutterComponentEx gutter = (EditorGutterComponentEx) editor.getGutter();
        int line = (int) gutter.getClientProperty("active.line.number");

        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        breakpointManager.addLineBreakpoint(savepointType, file.getUrl(), line, savepointType.createProperties());
    }
}
