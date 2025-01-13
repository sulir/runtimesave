package com.github.sulir.runtimesave;

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

        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createConfiguration(
                "RuntimeSave Starter", ApplicationConfigurationType.class);
        ApplicationConfiguration config = (ApplicationConfiguration) settings.getConfiguration();

        configureMain(config, className, methodName, paramsSignature);
        String agentJar = configureAgent(config);
        configureClasspath(config, agentJar);

        Executor debugExecutor = DefaultDebugExecutor.getDebugExecutorInstance();
        ProgramRunnerUtil.executeConfiguration(settings, debugExecutor);
    }

    private void configureMain(ApplicationConfiguration config, String className, String methodName,
                               String paramsSignature) {
        config.setMainClassName("com.github.sulir.runtimesave.starter.StarterMain");
        String programArgs = String.format("%s %s %s", className, methodName, paramsSignature);
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
