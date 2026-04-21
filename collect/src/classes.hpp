#pragma once

#include <jni.h>
#include <mutex>
#include <unordered_set>
#include <vector>

#include "buffer.hpp"

struct ClassState {
    jweak weakRef = nullptr;
    bool sent = false;
};

inline class ClassCache {
public:
    jclass objectClass = nullptr;
    static constexpr jlong CLASS_TAG = 1;
    static constexpr jlong STRING_TAG = 2;
    static constexpr jlong FIRST_FREE = 3;
    ClassCache();
    bool load(JNIEnv *jni);
    void unload(JNIEnv *jni);
    jlong addIfAbsent(jclass klass, JNIEnv *jni);
    bool addSafe(jlong *tag);
    void addClassObjectOnly(jlong *tag);
    jclass release(jlong tag, JNIEnv *jni);
    std::mutex& mutex();
private:
    jlong add(jclass klass, jlong tag, JNIEnv *jni);
    std::vector<ClassState> classes;
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

void addJavaLangClass(Buffer& buffer);
void loadClassesInfo(const std::vector<jlong>& newClasses, Buffer& buffer, JNIEnv *jni);
