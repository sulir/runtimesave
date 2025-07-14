package com.github.sulir.runtimesave.sample;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

public class SamplingRunner extends GenericDebuggerRunner {
    public static final long UID = -4814295447389679366L;

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(SamplingExecutor.EXECUTOR_ID) && profile instanceof JavaRunConfigurationBase;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        environment.setExecutionId(UID);
        super.execute(environment);
    }
}
