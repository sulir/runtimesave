package com.github.sulir.runtimesave.runtime;

import com.github.sulir.runtimesave.instrument.Settings;

import java.util.Arrays;

@SuppressWarnings("unused")
public class Collector {
    private static byte[] hits = new byte[512 * 1024];

    public static void collect(int lineId) {
        if (hits[lineId] >= Settings.HITS)
            return;
        hits[lineId]++;
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId) {
        if (oldLineId != newLineId)
            collect(newLineId);
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId, int compactLineId) {
        if (oldLineId != newLineId)
            collect(compactLineId);
    }

    public static void collectInfinity(int lineId) {
    }

    public static void collectInfinityIfLineChanged(int oldLineId, int newLineId) {
        if (oldLineId != newLineId)
            collectInfinity(newLineId);
    }

    public static void collectInfinityIfLineChanged(int oldLineId, int newLineId, int compactLineId) {
        if (oldLineId != newLineId)
            collectInfinity(compactLineId);
    }

    public static void enlargeHitsIfNeeded(int newSize) {
        if (newSize > hits.length)
            hits = Arrays.copyOf(hits, 2 * hits.length);
    }
}
