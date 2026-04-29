package io.github.sulir.runtimesave.config;

import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.classFilter.ClassFilter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeSaveSettingsTest {
    @Test
    void getOrDefaultOnEmptyDataReturnsDefaults() {
        RuntimeSaveSettings empty = RuntimeSaveSettings.getOrDefault(new UserDataHolderBase());
        RuntimeSaveSettings defaultSettings = new RuntimeSaveSettings();
        assertTrue(settingsEqual(defaultSettings, empty));
    }

    @Test
    void getOrDefaultReturnsGivenSettings() {
        RuntimeSaveSettings expected = new RuntimeSaveSettings();
        expected.setEveryNthLine(2);
        expected.setFirstTExecutions(3);
        expected.setIncludeFilters(new ClassFilter[]{ new ClassFilter("Pattern") });
        UserDataHolderBase data = new UserDataHolderBase();
        data.putUserData(RuntimeSaveSettings.key, expected);

        RuntimeSaveSettings settings = RuntimeSaveSettings.getOrDefault(data);
        assertTrue(settingsEqual(expected, settings));
    }

    @Test
    void enablingSavepointChangesDefaults() {
        RuntimeSaveSettings defaultSettings = new RuntimeSaveSettings();
        RuntimeSaveSettings enabled = new RuntimeSaveSettings();
        enabled.setSavepointEnabled(true);
        assertFalse(settingsEqual(defaultSettings, enabled));
    }

    @Test
    void classPatternHasUnchangedFormat() {
        RuntimeSaveSettings settings = new RuntimeSaveSettings();
        String pattern = "com.company.*";
        settings.setIncludeFilters(new ClassFilter[]{ new ClassFilter(pattern) });
        assertEquals(pattern, settings.getIncludeClassPatterns()[0]);
    }

    @Test
    void defaultSettingsIncludeAllClasses() {
        assertEquals(".*", new RuntimeSaveSettings().getIncludeRegex());
    }

    @Test
    void twoClassFiltersAreConvertedToRegex() {
        RuntimeSaveSettings settings = new RuntimeSaveSettings();
        ClassFilter[] filters = new ClassFilter[]{ new ClassFilter("a.*"), new ClassFilter("B") };
        settings.setIncludeFilters(filters);
        assertEquals("a\\..*|B", settings.getIncludeRegex());
    }

    private boolean settingsEqual(RuntimeSaveSettings settings, RuntimeSaveSettings other) {
        return settings.isSavepointEnabled() == other.isSavepointEnabled()
                && settings.getEveryNthLine() == other.getEveryNthLine()
                && settings.getFirstTExecutions() == other.getFirstTExecutions()
                && Arrays.equals(settings.getIncludeFilters(), other.getIncludeFilters());
    }
}
