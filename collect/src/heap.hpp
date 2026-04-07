#pragma once

#include <jni.h>
#include <vector>

#include "buffer.hpp"

struct HeapData {
    Buffer& buffer;
    std::vector<jint>& newClasses;
};

bool readHeap(const std::vector<jobject>& objects, Buffer& buffer, std::vector<jint>& newClasses, JNIEnv *jni);
