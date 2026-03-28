#pragma once

#include <atomic>
#include <chrono>
#include <cstdio>
#include <jni.h>
#include <jvmti.h>
#include <source_location>

extern jvmtiEnv *ti;

template <typename T>
bool ok(T err, jvmtiError allowedErr = JVMTI_ERROR_NONE, std::source_location loc = std::source_location::current()) {
    if (err && err != allowedErr) {
        fprintf(stderr, "Error %d at %s:%d\n", err, loc.file_name(), loc.line());
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

template <int Id = 0>
struct MethodTime {
    using Clock = std::chrono::steady_clock;
    std::chrono::time_point<Clock> start = Clock::now();
    inline static std::atomic<long long> total{0};
    
    ~MethodTime() {
        auto end = Clock::now();
        auto diff = std::chrono::nanoseconds(end - start).count();
        total.fetch_add(diff, std::memory_order_relaxed);
        (void) reportAtProgramExit;
    }
    inline static struct Reporter {
        ~Reporter() {
            fprintf(stderr, "Timer %d: %lld ms\n", Id, total / 1'000'000);
        }
    } reportAtProgramExit;
};
