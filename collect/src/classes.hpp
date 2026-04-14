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
    jlong add(jclass klass, JNIEnv *jni);
    bool contains(jlong tag);
    jweak get(jlong tag);
private:
    std::vector<jweak> classes;
    jlong nextTag = MIN_TAG;
    std::mutex mtx;
} classCache;

struct PrimitiveType {
    const char *name;
    jint size;
};

constexpr auto primitiveTypes = [] {
    std::array<PrimitiveType, 'Z' - 'B' + 1> arr{};
    arr['Z' - 'B'] = {"boolean", sizeof(jboolean)};
    arr['B' - 'B'] = {"byte", sizeof(jbyte)};
    arr['C' - 'B'] = {"char", sizeof(jchar)};
    arr['S' - 'B'] = {"short", sizeof(jshort)};
    arr['I' - 'B'] = {"int", sizeof(jint)};
    arr['J' - 'B'] = {"long", sizeof(jlong)};
    arr['F' - 'B'] = {"float", sizeof(jfloat)};
    arr['D' - 'B'] = {"double", sizeof(jdouble)};
    return arr;
}();

void loadClassesInfo(const std::vector<jweak>& cached, std::unordered_set<jlong>& uncached, Buffer& buffer,
        JNIEnv *jni);
