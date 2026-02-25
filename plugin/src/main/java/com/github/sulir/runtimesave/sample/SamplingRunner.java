package com.github.sulir.runtimesave.sample;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.configurations.RunProfile;
import org.jetbrains.annotations.NotNull;

public class SamplingRunner extends GenericDebuggerRunner {
    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(SamplingExecutor.EXECUTOR_ID)
                && profile instanceof CommonJavaRunConfigurationParameters;
    }
}
