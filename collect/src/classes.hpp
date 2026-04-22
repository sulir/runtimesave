#pragma once

#include <jni.h>
#include <mutex>
#include <unordered_set>
#include <vector>

#include "buffer.hpp"

struct PrimitiveType {
    const char *name;
    jint size;
    const char *boxed;
};

constexpr size_t PRIMITIVE_COUNT = 9;
constexpr auto primitiveTypes = [] {
    std::array<PrimitiveType, 'Z' - 'B' + 1> arr{};
    arr['Z' - 'B'] = {"boolean", sizeof(jboolean), "java/lang/Boolean"};
    arr['B' - 'B'] = {"byte", sizeof(jbyte), "java/lang/Byte"};
    arr['C' - 'B'] = {"char", sizeof(jchar), "java/lang/Character"};
    arr['S' - 'B'] = {"short", sizeof(jshort), "java/lang/Short"};
    arr['I' - 'B'] = {"int", sizeof(jint), "java/lang/Integer"};
    arr['J' - 'B'] = {"long", sizeof(jlong), "java/lang/Long"};
    arr['F' - 'B'] = {"float", sizeof(jfloat), "java/lang/Float"};
    arr['D' - 'B'] = {"double", sizeof(jdouble), "java/lang/Double"};
    arr['V' - 'B'] = {"void", 0, "java/lang/Void"};
    return arr;
}();

enum class SendState {
    None,
    Name,
    All
};

struct WeakClassInfo {
    jweak weakRef = nullptr;
    SendState state = SendState::None;
};

struct ClassInfo {
    jclass klass;
    SendState state;
};

inline class ClassCache {
public:
    jclass objectClass = nullptr;
    static constexpr jlong CLASS_TAG = 1;
    static constexpr jlong STRING_TAG = 2;
    static constexpr jlong FIRST_FREE = STRING_TAG + PRIMITIVE_COUNT + 1;
    ClassCache();
    bool load(JNIEnv *jni);
    void unload(JNIEnv *jni);
    jlong addIfAbsent(jclass klass, JNIEnv *jni);
    bool addSafe(jlong *tag, SendState send);
    ClassInfo get(jlong tag, JNIEnv *jni);
    void release(jlong tag, JNIEnv *jni);
    std::mutex& mutex();
private:
    jlong add(jclass klass, jlong tag, JNIEnv *jni);
    std::vector<WeakClassInfo> classes;
    std::mutex mtx;
} classCache;

void loadClassesInfo(const std::vector<jlong>& newClasses, Buffer& buffer, JNIEnv *jni);
