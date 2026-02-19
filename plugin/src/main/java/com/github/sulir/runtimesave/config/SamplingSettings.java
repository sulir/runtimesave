package com.github.sulir.runtimesave.config;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.util.Key;

public class SamplingSettings {
    public static final Key<SamplingSettings> key = Key.create(SamplingSettings.class.getPackageName());

    private int everyNthLine = 1;
    private int firstTExecutions = 1;

    public static SamplingSettings getOrDefault(RunConfigurationBase<?> configuration) {
        return configuration.putUserDataIfAbsent(key, new SamplingSettings());
    }

    public int getEveryNthLine() {
        return everyNthLine;
    }

    public void setEveryNthLine(int everyNthLine) {
        this.everyNthLine = everyNthLine;
    }

    public int getFirstTExecutions() {
        return firstTExecutions;
    }

    public void setFirstTExecutions(int firstTExecutions) {
        this.firstTExecutions = firstTExecutions;
    }
}
