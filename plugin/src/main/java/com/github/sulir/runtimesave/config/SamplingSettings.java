package com.github.sulir.runtimesave.config;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.util.Key;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.PatternUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SamplingSettings {
    public static final Key<SamplingSettings> key = Key.create(SamplingSettings.class.getPackageName());

    private int everyNthLine = 1;
    private int firstTExecutions = 1;
    private ClassFilter[] includeFilters = new ClassFilter[0];

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

    public ClassFilter[] getIncludeFilters() {
        return includeFilters;
    }

    public void setIncludeFilters(ClassFilter[] includeFilters) {
        this.includeFilters = includeFilters;
    }

    public String getIncludePattern() {
        if (includeFilters.length == 0)
            return ".*";

        return Arrays.stream(includeFilters)
                .filter(ClassFilter::isEnabled)
                .map(ClassFilter::getPattern)
                .map(PatternUtil::convertToRegex)
                .collect(Collectors.joining("|"));
    }
}
