#pragma once

#include <atomic>
#include <chrono>
#include <iostream>
#include <jni.h>
#include <jvmti.h>
#include <source_location>

extern jvmtiEnv *ti;

inline bool jvmti_ok(jvmtiError err, jvmtiError allowedErr = JVMTI_ERROR_NONE,
                     std::source_location loc = std::source_location::current()) {
    if (err != JVMTI_ERROR_NONE && err != allowedErr) {
        fprintf(stderr, "JVMTI error %d at %s:%d\n", err, loc.file_name(), loc.line());
        return false;
    }
    return true;
}

inline bool jni_ok(jint err, std::source_location loc = std::source_location::current()) {
    if (err != JNI_OK) {
        fprintf(stderr, "JNI error %d at %s:%d\n", err, loc.file_name(), loc.line());
        return false;
    }
    return true;
}

template <typename T>
T jni_check(T value, JNIEnv *env) {
    if (!value && env->ExceptionCheck())
        env->ExceptionDescribe();
    return value;
} 

inline bool is_obsolete(jmethodID method) {
    jboolean obsolete;
    if (!jvmti_ok(ti->IsMethodObsolete(method, &obsolete)))
        return true;
    return obsolete;
}

template <typename T>
void dealloc(T *ptr) {
    if (ti)
        ti->Deallocate(reinterpret_cast<unsigned char *>(ptr));
}

template <typename T>
class JvmtiDealloc {
public:
    explicit JvmtiDealloc(): ptr(nullptr) {}
    ~JvmtiDealloc() { dealloc(ptr); }

    T **out() { return &ptr; }
    T *get() { return ptr; }

    JvmtiDealloc(const JvmtiDealloc&) = delete;
    JvmtiDealloc& operator=(const JvmtiDealloc&) = delete;
private:
    T *ptr;
};

struct MethodTime {
    using Clock = std::chrono::steady_clock;
    std::chrono::time_point<Clock> start = Clock::now();
    inline static std::atomic<long long> total{0};
    
    ~MethodTime() {
        auto end = Clock::now();
        auto diff = std::chrono::nanoseconds(end - start).count();
        total.fetch_add(diff, std::memory_order_relaxed);
    }
    inline static struct Reporter {
        ~Reporter() {
            if (total != 0)
                std::cerr << total / 1'000'000 << " ms\n";
        }
    } reporter;
};
