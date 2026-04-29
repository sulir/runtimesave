package io.github.sulir.runtimesave.config;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.PatternUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RuntimeSaveSettings {
    public static final Key<RuntimeSaveSettings> key = Key.create(RuntimeSaveSettings.class.getPackageName());

    private boolean savepointEnabled = false;
    private int everyNthLine = 1;
    private int firstTExecutions = 1;
    private ClassFilter[] includeFilters = new ClassFilter[0];

    public static RuntimeSaveSettings getOrDefault(UserDataHolderEx configuration) {
        return configuration.putUserDataIfAbsent(key, new RuntimeSaveSettings());
    }

    public boolean isSavepointEnabled() {
        return savepointEnabled;
    }

    public void setSavepointEnabled(boolean savepointEnabled) {
        this.savepointEnabled = savepointEnabled;
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

    public String[] getIncludeClassPatterns() {
        return Arrays.stream(includeFilters)
                .filter(ClassFilter::isEnabled)
                .map(ClassFilter::getPattern)
                .toArray(String[]::new);
    }

    public String getIncludeRegex() {
        if (includeFilters.length == 0)
            return ".*";

        return Arrays.stream(includeFilters)
                .filter(ClassFilter::isEnabled)
                .map(ClassFilter::getPattern)
                .map(PatternUtil::convertToRegex)
                .collect(Collectors.joining("|"));
    }
}
