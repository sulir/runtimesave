package io.github.sulir.runtimesave.sample;

import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.TextWithMnemonic;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SamplingDebugExecutor extends DefaultDebugExecutor {
    public static final String EXECUTOR_ID = "SamplingDebug";

    @Override
    public @NotNull String getActionName() {
        return "Sample Values (Debug)";
    }

    @Override
    public @NotNull String getStartActionText() {
        return "&Sample Values (Debug)";
    }

    @Override
    public @NotNull String getStartActionText(@NotNull String configurationName) {
        return TextWithMnemonic.parse("&Sample Values from '%s' (Debug)")
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
