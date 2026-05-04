package io.github.sulir.runtimesave.config;

import com.intellij.execution.Executor;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.util.xmlb.XmlSerializer;
import io.github.sulir.runtimesave.sample.SamplingDebugExecutor;
import io.github.sulir.runtimesave.sample.SamplingRunExecutor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.Objects;

public class RuntimeSaveRunConfigExtension extends RunConfigurationExtension {
    @Override
    public void updateJavaParameters(@NotNull RunConfigurationBase configuration,
                                     @NotNull JavaParameters params, @Nullable RunnerSettings runnerSettings) {

    }

    @Override
    public void updateJavaParameters(@NonNull RunConfigurationBase configuration, @NotNull JavaParameters params,
                                     RunnerSettings runnerSettings, @NotNull Executor executor) {
        RuntimeSaveSettings settings = RuntimeSaveSettings.getOrDefault(configuration);
        boolean sampling = executor.getId().equals(SamplingRunExecutor.EXECUTOR_ID)
                || executor.getId().equals(SamplingDebugExecutor.EXECUTOR_ID);

        if (sampling || settings.isSavepointEnabled()) {
            ParametersList vm = params.getVMParametersList();
            PluginDescriptor plugin = Objects.requireNonNull(PluginManager.getPluginByClass(getClass()));
            Path agentsPath = plugin.getPluginPath().resolve("agent");
            vm.add("-javaagent:" + agentsPath.resolve("runtimesave-instrument.jar"));
            vm.add("-agentpath:" + agentsPath.resolve(System.mapLibraryName("runtimesave")));

            if (sampling) {
                vm.addProperty("runtimesave.line", String.valueOf(settings.getEveryNthLine()));
                vm.addProperty("runtimesave.hits", String.valueOf(settings.getFirstTExecutions()));
                vm.addProperty("runtimesave.include", settings.getIncludeRegex());
            } else {
                vm.addProperty("runtimesave.include", "(?!)");
            }
        }
    }

    @Override
    protected @Nullable String getEditorTitle() {
        return "RuntimeSave";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected SettingsEditor<RunConfigurationBase<?>> createEditor(@NotNull RunConfigurationBase configuration) {
        return new RuntimeSaveSettingsEditor(configuration.getProject());
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase configuration) {
        return true;
    }

    @Override
    protected void readExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        RuntimeSaveSettings settings = XmlSerializer.deserialize(element, RuntimeSaveSettings.class);
        configuration.putUserData(RuntimeSaveSettings.key, settings);
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        XmlSerializer.serializeInto(RuntimeSaveSettings.getOrDefault(configuration), element);
    }
}
