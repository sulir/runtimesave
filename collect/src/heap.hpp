#pragma once

#include <atomic>
#include <jni.h>
#include <vector>

#include "buffer.hpp"

static std::atomic<jlong> nextSequenceNum{1};

struct HeapData {
    Buffer& buffer;
    jlong sequenceNum;
    jlong newClassesStart;
    jint newClassesCount;
};

bool readHeap(const std::vector<jobject>& objects, HeapData& heapData, JNIEnv *jni);
