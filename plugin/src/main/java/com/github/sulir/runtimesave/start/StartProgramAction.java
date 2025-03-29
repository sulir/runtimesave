package com.github.sulir.runtimesave.start;

import com.intellij.execution.Executor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.ide.plugins.cl.PluginAwareClassLoader;
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

import java.nio.file.Path;
import java.util.List;

public class StartProgramAction extends AnAction {
    public static final String MAIN_CLASS = "com.github.sulir.runtimesave.starter.StarterMain";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiMethod method = getMethodAtCursor(e.getProject());
        Integer line = getLineAtCursor(e.getProject());

        if (method != null && line != null)
            startDebugging(method, line);
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

    private Integer getLineAtCursor(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null)
            return null;

        return editor.getCaretModel().getLogicalPosition().line + 1;
    }

    private void startDebugging(PsiMethod method, int line) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null)
            return;

        String className = ClassUtil.getJVMClassName(containingClass);
        String methodName = method.getName();
        String descriptor = ClassUtil.getAsmMethodSignature(method);

        RunnerAndConfigurationSettings settings = RunManager.getInstance(method.getProject()).createConfiguration(
                "RuntimeSave Starter", ApplicationConfigurationType.class);
        ApplicationConfiguration config = (ApplicationConfiguration) settings.getConfiguration();

        configureMain(config, className, methodName, descriptor, line);
        String agentJar = configureAgent(config);
        configureClasspath(config, agentJar);

        Executor debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance();
        ProgramRunnerUtil.executeConfiguration(settings, debugExecutor);
    }

    private void configureMain(ApplicationConfiguration config, String className, String methodName,
                               String descriptor, int line) {
        config.setMainClassName(MAIN_CLASS);
        String programArgs = String.format("%s %s %s %d", className, methodName, descriptor, line);
        config.setProgramParameters(programArgs);
    }

    private String configureAgent(ApplicationConfiguration config) {
        PluginAwareClassLoader thisLoader = (PluginAwareClassLoader) this.getClass().getClassLoader();
        Path pluginPath = thisLoader.getPluginDescriptor().getPluginPath();
        String agentJar = pluginPath.resolve("lib").resolve("runtimesave-starter.jar").toString();
        String vmArgs = String.format("-javaagent:%s", agentJar);
        config.setVMParameters(vmArgs);
        return agentJar;
    }

    private void configureClasspath(ApplicationConfiguration config, String agentJar) {
        var includeStarterClasspath = new ModuleBasedConfigurationOptions.ClasspathModification(agentJar, false);
        config.setClasspathModifications(List.of(includeStarterClasspath));
    }
}
