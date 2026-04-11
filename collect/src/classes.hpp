#pragma once

#include <jni.h>
#include <mutex>
#include <unordered_set>
#include <vector>

#include "buffer.hpp"

inline class ClassCache {
public:
    static constexpr jlong MIN_TAG = 1LL << 62;
    ClassCache();
    void add(jclass klass, JNIEnv *jni);
    jweak get(jlong tag);
private:
    std::vector<jweak> classes;
    jlong nextTag = MIN_TAG;
    std::mutex mtx;
} classCache;

void loadClassesInfo(const std::vector<jweak>& cached, std::unordered_set<jlong>& uncached, Buffer& buffer,
        JNIEnv *jni);
