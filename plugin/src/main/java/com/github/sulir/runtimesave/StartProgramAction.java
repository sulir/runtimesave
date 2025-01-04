package com.github.sulir.runtimesave;

import com.intellij.execution.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class StartProgramAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiMethod method = getMethodAtCursor(e.getProject());
        startDebugging(e.getProject(), method);
    }

    private PsiMethod getMethodAtCursor(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        int offset = editor.getCaretModel().getOffset();

        PsiElement element = PsiManager.getInstance(project).findFile(file).findElementAt(offset);
        return PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    }

    private void startDebugging(Project project, PsiMethod method) {
        String className = ClassUtil.getJVMClassName(method.getContainingClass());
        String methodName = method.getName();
        String methodSignature = ClassUtil.getAsmMethodSignature(method);
        String paramsSignature = methodSignature.substring(0, methodSignature.lastIndexOf(')') + 1);

        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings selected = runManager.findConfigurationByName("ProgramStarter");
        String programArgs = String.format("\"%s\" \"%s\" \"%s\"", className, methodName, paramsSignature);
        ((JavaRunConfigurationBase) selected.getConfiguration()).setProgramParameters(programArgs);
        Executor debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance();
        ProgramRunnerUtil.executeConfiguration(selected, debugExecutor);
    }
}
