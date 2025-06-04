package com.github.sulir.runtimesave;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.ide.plugins.cl.PluginAwareClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class HelperRunConfigExtension extends RunConfigurationExtension {
    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@NotNull T configuration,
             @NotNull JavaParameters params, @Nullable RunnerSettings runnerSettings) {
        PluginAwareClassLoader thisLoader = (PluginAwareClassLoader) this.getClass().getClassLoader();
        Path pluginPath = thisLoader.getPluginDescriptor().getPluginPath();
        String starterJar = pluginPath.resolve("lib").resolve("runtimesave-starter.jar").toString();
        params.getClassPath().add(starterJar);

        params.getVMParametersList().add("-javaagent:" + starterJar);
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> runConfigurationBase) {
        return true;
    }
}
