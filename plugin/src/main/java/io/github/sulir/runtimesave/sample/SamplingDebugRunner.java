package io.github.sulir.runtimesave.sample;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

public class SamplingDebugRunner extends GenericDebuggerRunner {
    public static final String RUNNER_ID = "SamplingDebug";
    public static final long UID = -4814295447389679366L;

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(SamplingDebugExecutor.EXECUTOR_ID)
                && profile instanceof CommonJavaRunConfigurationParameters;
    }

    @Override
    public @NotNull String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        environment.setExecutionId(UID);
        super.execute(environment);
    }
}
