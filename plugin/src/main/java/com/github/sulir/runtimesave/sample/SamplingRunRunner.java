package com.github.sulir.runtimesave.sample;

import com.github.sulir.runtimesave.config.SamplingSettings;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.ide.plugins.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

public class SamplingRunRunner extends DefaultJavaProgramRunner {
    public static final String RUNNER_ID = "SamplingRun";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(SamplingRunExecutor.EXECUTOR_ID)
                && profile instanceof CommonJavaRunConfigurationParameters;
    }

    @Override
    public @NotNull String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public void patch(@NotNull JavaParameters javaParameters, @Nullable RunnerSettings runnerSettings, @NotNull RunProfile runProfile, boolean beforeExecution) {
        ParametersList vm = javaParameters.getVMParametersList();
        Path pluginPath = Objects.requireNonNull(PluginManager.getPluginByClass(getClass())).getPluginPath();
        String agentPath = pluginPath.resolve("lib").resolve("runtimesave-instrument.jar").toString();
        vm.add("-javaagent:" + agentPath);

        SamplingSettings settings = SamplingSettings.getOrDefault((RunConfigurationBase<?>) runProfile);
        vm.addProperty("runtimesave.n", String.valueOf(settings.getEveryNthLine()));
        vm.addProperty("runtimesave.t", String.valueOf(settings.getFirstTExecutions()));
        vm.addProperty("runtimesave.include", settings.getIncludeRegex());

        super.patch(javaParameters, runnerSettings, runProfile, beforeExecution);
    }
}
