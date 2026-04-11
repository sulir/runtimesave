#pragma once

#include <atomic>
#include <jni.h>
#include <unordered_set>
#include <vector>

#include "buffer.hpp"

static std::atomic<jlong> nextSequenceNum{1};

struct HeapData {
    Buffer& buffer;
    jlong sequenceNum = 0;
    std::vector<jweak> cachedClasses{};
    std::unordered_set<jlong> uncachedClasses{};
};

bool readHeap(const std::vector<jobject>& objects, HeapData& heapData, JNIEnv *jni);
