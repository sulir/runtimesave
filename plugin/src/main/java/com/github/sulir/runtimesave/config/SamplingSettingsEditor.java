package com.github.sulir.runtimesave.config;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.ui.JBIntSpinner;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SamplingSettingsEditor extends SettingsEditor<RunConfigurationBase<?>> {
    private JPanel mainPanel;
    private JBIntSpinner everyNthLineSpinner;
    private JBIntSpinner firstTExecutionsSpinner;

    @Override
    protected void resetEditorFrom(@NotNull RunConfigurationBase configuration) {
        SamplingSettings settings = SamplingSettings.getOrDefault(configuration);
        everyNthLineSpinner.setValue(settings.getEveryNthLine());
        firstTExecutionsSpinner.setValue(settings.getFirstTExecutions());
    }

    @Override
    protected void applyEditorTo(@NotNull RunConfigurationBase s) {
        SamplingSettings settings = SamplingSettings.getOrDefault(s);
        settings.setEveryNthLine(everyNthLineSpinner.getNumber());
        settings.setFirstTExecutions(firstTExecutionsSpinner.getNumber());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return mainPanel;
    }

    private void createUIComponents() {
        SamplingSettings defaults = new SamplingSettings();
        everyNthLineSpinner = new JBIntSpinner(defaults.getEveryNthLine(), 1, Integer.MAX_VALUE);
        firstTExecutionsSpinner = new JBIntSpinner(defaults.getFirstTExecutions(), -1, Integer.MAX_VALUE);
    }
}
