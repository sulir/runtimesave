package io.github.sulir.runtimesave.sample;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import org.jetbrains.annotations.NotNull;

public class SamplingRunRunner extends DefaultJavaProgramRunner {
    public static final String RUNNER_ID = "SamplingRun";

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(SamplingRunExecutor.EXECUTOR_ID)
                && profile instanceof CommonJavaRunConfigurationParameters;
    }

    @Override
    public @NotNull String getRunnerId() {
        return RUNNER_ID;
    }
}
