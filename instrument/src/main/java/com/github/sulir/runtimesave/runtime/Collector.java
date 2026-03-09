package com.github.sulir.runtimesave.runtime;

@SuppressWarnings("unused")
public class Collector {
    public static void collect(int lineId) {
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId) {
        if (oldLineId != newLineId)
            collect(newLineId);
    }

    public static void collectIfLineChanged(int oldLineId, int newLineId, int compactLineId) {
        if (oldLineId != newLineId)
            collect(compactLineId);
    }
}
