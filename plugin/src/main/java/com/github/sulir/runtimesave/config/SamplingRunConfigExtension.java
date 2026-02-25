package com.github.sulir.runtimesave.config;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SamplingRunConfigExtension extends RunConfigurationExtension {
    @Override
    public void updateJavaParameters(@NotNull RunConfigurationBase configuration,
                                     @NotNull JavaParameters params, @Nullable RunnerSettings runnerSettings) {
    }

    @Override
    protected @Nullable String getEditorTitle() {
        return "RuntimeSave Sampling";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected SettingsEditor<RunConfigurationBase<?>> createEditor(@NotNull RunConfigurationBase configuration) {
        return new SamplingSettingsEditor(configuration.getProject());
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase configuration) {
        return true;
    }

    @Override
    protected void readExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        SamplingSettings settings = XmlSerializer.deserialize(element, SamplingSettings.class);
        configuration.putUserData(SamplingSettings.key, settings);
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        XmlSerializer.serializeInto(SamplingSettings.getOrDefault(configuration), element);
    }
}
