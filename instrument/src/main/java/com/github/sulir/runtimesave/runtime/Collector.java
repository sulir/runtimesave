package com.github.sulir.runtimesave.runtime;

import com.github.sulir.runtimesave.instrument.Settings;

@SuppressWarnings("unused")
public class Collector {
    public static void collectInfinity() {
        doCollection();
    }

    public static void collect(int lineId, byte[] hits) {
        if (hits != null && hits[lineId] < Settings.HITS) {
            hits[lineId]++;
            doCollection();
        }
    }

    public static void collectInfinityIfLineChanged(int oldLineId, int newLineId) {
        if (oldLineId != newLineId)
            doCollection();
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId, byte[] hits) {
        if (oldLineId != newLineId && hits != null && hits[newLineId] < Settings.HITS) {
            hits[newLineId]++;
            doCollection();
        }
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId, byte[] hits, int compactLineId) {
        if (oldLineId != newLineId && hits != null && hits[compactLineId] < Settings.HITS) {
            hits[compactLineId]++;
            doCollection();
        }
    }

    private static void doCollection() {
    }
}
