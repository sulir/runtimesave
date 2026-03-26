package io.github.sulir.runtimesave.rt;

import io.github.sulir.runtimesave.SourceLocation;
import io.github.sulir.runtimesave.instrument.Settings;

import java.util.Arrays;

@SuppressWarnings("unused")
public class Collector {
    private static int[] hits = new int[128 * 1024];

    public static void collectInfinity() {
        collectData();
    }

    public static void collect(int lineId) {
        int count;
        if ((count = hits[lineId]) < Settings.HITS) {
            hits[lineId] = count + 1;
            collectData();
        }
    }

    public static void collectInfinityIfLineChanged(int oldLineId, int newLineId) {
        if (oldLineId != newLineId)
            collectData();
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId) {
        int count;
        if (oldLineId != newLineId && (count = hits[newLineId]) < Settings.HITS) {
            hits[newLineId] = count + 1;
            collectData();
        }
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId, int compactLineId) {
        int count;
        if (oldLineId != newLineId && (count = hits[compactLineId]) < Settings.HITS) {
            hits[compactLineId] = count + 1;
            collectData();
        }
    }

    public static void enlargeHitsIfNeeded(int newSize) {
        if (newSize > hits.length)
            hits = Arrays.copyOf(hits, 2 * hits.length);
    }

    private static void collectData() {
        SourceLocation location = findLocation();
        if (Settings.DEBUG)
            System.err.println(location);
        SaveService.getInstance().saveLocation(location);
    }

    private static native SourceLocation findLocation();
}
