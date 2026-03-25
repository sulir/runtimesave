package io.github.sulir.runtimesave.sample;

import io.github.sulir.runtimesave.config.SamplingSettings;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginDescriptor;
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
        PluginDescriptor plugin = Objects.requireNonNull(PluginManager.getPluginByClass(getClass()));
        Path agentsPath = plugin.getPluginPath().resolve("agent");
        vm.add("-javaagent:" + agentsPath.resolve("runtimesave-instrument.jar"));
        vm.add("-agentpath:" + agentsPath.resolve(System.mapLibraryName("runtimesave")));

        SamplingSettings settings = SamplingSettings.getOrDefault((RunConfigurationBase<?>) runProfile);
        vm.addProperty("runtimesave.line", String.valueOf(settings.getEveryNthLine()));
        vm.addProperty("runtimesave.hits", String.valueOf(settings.getFirstTExecutions()));
        vm.addProperty("runtimesave.include", settings.getIncludeRegex());

        super.patch(javaParameters, runnerSettings, runProfile, beforeExecution);
    }
}
