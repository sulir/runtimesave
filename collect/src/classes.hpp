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
    const char *array;
};

constexpr size_t PRIMITIVE_COUNT = 9;
constexpr size_t PRIMITIVE_ARR_COUNT = 8;
constexpr auto primitiveTypes = [] {
    std::array<PrimitiveType, 'Z' - 'B' + 1> arr{};
    arr['Z' - 'B'] = {"boolean", sizeof(jboolean), "java/lang/Boolean", "[Z"};
    arr['B' - 'B'] = {"byte", sizeof(jbyte), "java/lang/Byte", "[B"};
    arr['C' - 'B'] = {"char", sizeof(jchar), "java/lang/Character", "[C"};
    arr['S' - 'B'] = {"short", sizeof(jshort), "java/lang/Short", "[S"};
    arr['I' - 'B'] = {"int", sizeof(jint), "java/lang/Integer", "[I"};
    arr['J' - 'B'] = {"long", sizeof(jlong), "java/lang/Long", "[J"};
    arr['F' - 'B'] = {"float", sizeof(jfloat), "java/lang/Float", "[F"};
    arr['D' - 'B'] = {"double", sizeof(jdouble), "java/lang/Double", "[D"};
    arr['V' - 'B'] = {"void", 0, "java/lang/Void", nullptr};
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
    static constexpr jlong PRIMITIVE_ARR_MIN = STRING_TAG + PRIMITIVE_COUNT + 1;
    static constexpr jlong PRIMITIVE_ARR_MAX = PRIMITIVE_ARR_MIN + PRIMITIVE_ARR_COUNT - 1;
    static constexpr jlong FIRST_FREE = PRIMITIVE_ARR_MAX + 1;
    static constexpr size_t DEFAULT_SIZE = 32 * 1024;
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
