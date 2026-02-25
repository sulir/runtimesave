package com.github.sulir.runtimesave.sample;

import com.github.sulir.runtimesave.config.SamplingSettings;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.ide.plugins.PluginManager;

import java.nio.file.Path;
import java.util.Objects;

public class SamplingPatcher extends JavaProgramPatcher {
    @Override
    public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
        if (!executor.getId().equals(SamplingExecutor.EXECUTOR_ID) || System.getenv("RS_INSTRUMENT") == null)
            return;

        ParametersList vm = javaParameters.getVMParametersList();
        Path pluginPath = Objects.requireNonNull(PluginManager.getPluginByClass(getClass())).getPluginPath();
        String agentPath = pluginPath.resolve("lib").resolve("runtimesave-instrument.jar").toString();
        vm.add("-javaagent:" + agentPath);

        SamplingSettings settings = SamplingSettings.getOrDefault((RunConfigurationBase<?>) configuration);
        vm.addProperty("runtimesave.n", String.valueOf(settings.getEveryNthLine()));
        vm.addProperty("runtimesave.t", String.valueOf(settings.getFirstTExecutions()));
        vm.addProperty("runtimesave.include", settings.getIncludePattern());
    }
}
