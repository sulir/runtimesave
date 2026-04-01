package io.github.sulir.runtimesave.rt;

import io.github.sulir.runtimesave.instrument.Settings;
import io.github.sulir.runtimesave.misc.Log;

import java.nio.ByteBuffer;
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
        ByteBuffer buffer = readData();
        if (buffer != null)
            SaveService.getInstance().saveFrame(buffer);
        else
            Log.error("Cannot read runtime data");
    }

    private static native ByteBuffer readData();
}
