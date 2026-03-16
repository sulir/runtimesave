package com.github.sulir.runtimesave.runtime;

import com.github.sulir.runtimesave.instrument.Settings;

import java.util.Arrays;

@SuppressWarnings("unused")
public class Collector {
    private static int[] hits = new int[128 * 1024];

    public static void collectInfinity() {
        doCollection();
    }

    public static void collect(int lineId) {
        int count;
        if ((count = hits[lineId]) < Settings.HITS) {
            hits[lineId] = count + 1;
            doCollection();
        }
    }

    public static void collectInfinityIfLineChanged(int oldLineId, int newLineId) {
        if (oldLineId != newLineId)
            doCollection();
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId) {
        int count;
        if (oldLineId != newLineId && (count = hits[newLineId]) < Settings.HITS) {
            hits[newLineId] = count + 1;
            doCollection();
        }
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId, int compactLineId) {
        int count;
        if (oldLineId != newLineId && (count = hits[compactLineId]) < Settings.HITS) {
            hits[compactLineId] = count + 1;
            doCollection();
        }
    }

    public static void enlargeHitsIfNeeded(int newSize) {
        if (newSize > hits.length)
            hits = Arrays.copyOf(hits, 2 * hits.length);
    }

    private static void doCollection() {
    }
}
