package com.github.sulir.runtimesave.sample;

import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.TextWithMnemonic;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SamplingExecutor extends DefaultDebugExecutor {
    public static final String EXECUTOR_ID = "SampleValues";

    @Override
    public @NotNull String getActionName() {
        return "Sample Values";
    }

    @Override
    public @NotNull String getStartActionText() {
        return "&Sample Values";
    }

    @Override
    public @NotNull String getStartActionText(@NotNull String configurationName) {
        return TextWithMnemonic.parse("&Sample Values from '%s'")
                .replaceFirst("%s", shortenNameIfNeeded(configurationName)).toString();
    }

    @Override
    public @NotNull String getId() {
        return EXECUTOR_ID;
    }

    @Override
    public String getContextActionId() {
        return EXECUTOR_ID + "Context";
    }

    @Override
    public boolean isSupportedOnTarget() {
        return EXECUTOR_ID.equalsIgnoreCase(getId());
    }

    @Override
    public @NotNull Icon getIcon() {
        return AllIcons.Actions.MenuSaveall;
    }

    @Override
    public Icon getDisabledIcon() {
        return IconLoader.getDisabledIcon(AllIcons.Actions.MenuSaveall);
    }

    @Override
    public @NotNull String getToolWindowId() {
        return EXECUTOR_ID;
    }

    @Override
    public @NotNull Icon getToolWindowIcon() {
        return AllIcons.Actions.MenuSaveall;
    }
}
