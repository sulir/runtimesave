package io.github.sulir.runtimesave.config;

import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.classFilter.ClassFilter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamplingSettingsTest {
    @Test
    void getOrDefaultOnEmptyDataReturnsDefaults() {
        SamplingSettings empty = SamplingSettings.getOrDefault(new UserDataHolderBase());
        SamplingSettings defaultSettings = new SamplingSettings();
        assertTrue(settingsEqual(defaultSettings, empty));
    }

    @Test
    void getOrDefaultReturnsGivenSettings() {
        SamplingSettings expected = new SamplingSettings();
        expected.setEveryNthLine(2);
        expected.setFirstTExecutions(3);
        expected.setIncludeFilters(new ClassFilter[]{ new ClassFilter("Pattern") });
        UserDataHolderBase data = new UserDataHolderBase();
        data.putUserData(SamplingSettings.key, expected);

        SamplingSettings settings = SamplingSettings.getOrDefault(data);
        assertTrue(settingsEqual(expected, settings));
    }

    @Test
    void classPatternHasUnchangedFormat() {
        SamplingSettings settings = new SamplingSettings();
        String pattern = "com.company.*";
        settings.setIncludeFilters(new ClassFilter[]{ new ClassFilter(pattern) });
        assertEquals(pattern, settings.getIncludeClassPatterns()[0]);
    }

    @Test
    void defaultSettingsIncludeAllClasses() {
        assertEquals(".*", new SamplingSettings().getIncludeRegex());
    }

    @Test
    void twoClassFiltersAreConvertedToRegex() {
        SamplingSettings settings = new SamplingSettings();
        ClassFilter[] filters = new ClassFilter[]{ new ClassFilter("a.*"), new ClassFilter("B") };
        settings.setIncludeFilters(filters);
        assertEquals("a\\..*|B", settings.getIncludeRegex());
    }

    private boolean settingsEqual(SamplingSettings settings, SamplingSettings other) {
        return settings.getEveryNthLine() == other.getEveryNthLine()
                && settings.getFirstTExecutions() == other.getFirstTExecutions()
                && Arrays.equals(settings.getIncludeFilters(), other.getIncludeFilters());
    }
}
