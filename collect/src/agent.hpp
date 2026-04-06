#pragma once

#include <atomic>
#include <chrono>
#include <cstdio>
#include <limits>
#include <jni.h>
#include <jvmti.h>
#include <source_location>

constexpr int CALLER_DEPTH = 3;
inline jvmtiEnv *ti = nullptr;
inline std::atomic<jlong> nextObjectTag{1};

template <typename T>
bool ok(T err, jvmtiError allowedErr = JVMTI_ERROR_NONE, std::source_location loc = std::source_location::current()) {
    if (err && err != allowedErr) {
        std::fprintf(stderr, "Error %d at %s:%d\n", err, loc.file_name(), loc.line());
        return false;
    }
    return true;
}

template <typename T>
T jniCatch(T value, JNIEnv *env) {
    if (!value && env->ExceptionCheck())
        env->ExceptionDescribe();
    return value;
}

template <typename T>
T replaceWithGlobal(T localRef, JNIEnv *env) {
    if (!localRef)
        return nullptr;
    T global = static_cast<T>(env->NewGlobalRef(localRef));
    env->DeleteLocalRef(localRef);
    return global;
}

template <typename T>
void dealloc(T *ptr) {
    if (ti)
        ti->Deallocate(reinterpret_cast<unsigned char *>(ptr));
}

inline struct SystemClasses {
    jclass objectClass = nullptr;
    const jlong STRING_TAG = std::numeric_limits<jlong>::min();

    void load(JNIEnv *jni) {
        objectClass = replaceWithGlobal(jniCatch(jni->FindClass("java/lang/Object"), jni), jni);

        jclass stringClass = jniCatch(jni->FindClass("java/lang/Object"), jni);
        if (stringClass)
            ok(ti->SetTag(stringClass, STRING_TAG));
    }
    void unload(JNIEnv *jni) {
        jni->DeleteGlobalRef(objectClass);
    }
} systemClasses;

template <int Id = 0>
class ScopeTime {
    using Clock = std::chrono::steady_clock;
    std::chrono::time_point<Clock> start = Clock::now();
    inline static std::atomic<long long> total{0};
    inline static struct Reporter {
        ~Reporter() {
            std::fprintf(stderr, "C++ timer %d: %lld ms\n", Id, total / 1'000'000);
        }
    } reportAtProgramExit;
public:
    ~ScopeTime() {
        auto end = Clock::now();
        auto diff = std::chrono::nanoseconds(end - start).count();
        total.fetch_add(diff, std::memory_order_relaxed);
        (void) reportAtProgramExit;
    }
};
