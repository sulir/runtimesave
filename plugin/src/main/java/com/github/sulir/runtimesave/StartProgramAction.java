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
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class StartProgramAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiMethod method = getMethodAtCursor(e.getProject());
        if (method != null)
            startDebugging(e.getProject(), method);
    }

    private PsiMethod getMethodAtCursor(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null)
            return null;

        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (virtualFile == null)
            return null;

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null)
            return null;

        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    }

    private void startDebugging(Project project, PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null)
            return;

        String className = ClassUtil.getJVMClassName(containingClass);
        String methodName = method.getName();
        String methodSignature = ClassUtil.getAsmMethodSignature(method);
        String paramsSignature = methodSignature.substring(0, methodSignature.lastIndexOf(')') + 1);

        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings selected = runManager.findConfigurationByName("ProgramStarter");
        if (selected == null)
            return;

        String programArgs = String.format("\"%s\" \"%s\" \"%s\"", className, methodName, paramsSignature);
        ((JavaRunConfigurationBase) selected.getConfiguration()).setProgramParameters(programArgs);
        Executor debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance();
        ProgramRunnerUtil.executeConfiguration(selected, debugExecutor);
    }
}
