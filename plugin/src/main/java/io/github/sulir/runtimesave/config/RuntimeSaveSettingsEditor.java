package io.github.sulir.runtimesave.config;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBIntSpinner;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RuntimeSaveSettingsEditor extends SettingsEditor<RunConfigurationBase<?>> {
    private final Project project;
    private JPanel mainPanel;
    private JBIntSpinner everyNthLineSpinner;
    private JBIntSpinner firstTExecutionsSpinner;
    private ProjectPackagesFilterEditor filterEditor;
    private JCheckBox savepointEnabledCheckBox;

    public RuntimeSaveSettingsEditor(Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(@NotNull RunConfigurationBase configuration) {
        RuntimeSaveSettings settings = RuntimeSaveSettings.getOrDefault(configuration);
        savepointEnabledCheckBox.setSelected(settings.isSavepointEnabled());
        everyNthLineSpinner.setValue(settings.getEveryNthLine());
        firstTExecutionsSpinner.setValue(settings.getFirstTExecutions());
        filterEditor.setFilters(settings.getIncludeFilters());
    }

    @Override
    protected void applyEditorTo(@NotNull RunConfigurationBase s) {
        RuntimeSaveSettings settings = RuntimeSaveSettings.getOrDefault(s);
        settings.setSavepointEnabled(savepointEnabledCheckBox.isSelected());
        settings.setEveryNthLine(everyNthLineSpinner.getNumber());
        settings.setFirstTExecutions(firstTExecutionsSpinner.getNumber());
        settings.setIncludeFilters(filterEditor.getFilters());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return mainPanel;
    }

    private void createUIComponents() {
        RuntimeSaveSettings defaults = new RuntimeSaveSettings();
        everyNthLineSpinner = new JBIntSpinner(defaults.getEveryNthLine(), 1, Integer.MAX_VALUE);
        firstTExecutionsSpinner = new JBIntSpinner(defaults.getFirstTExecutions(), -1, Integer.MAX_VALUE);
        filterEditor = new ProjectPackagesFilterEditor(project);
    }
}
