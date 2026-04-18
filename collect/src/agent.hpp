#pragma once

#include <atomic>
#include <chrono>
#include <cstdio>
#include <limits>
#include <jni.h>
#include <jvmti.h>
#include <source_location>
#include <utility>

constexpr int CALLER_DEPTH = 3;
inline jvmtiEnv *ti = nullptr;

inline struct Registry {
    jclass objectClass = nullptr;
    jlong classTag = 0;
    static constexpr jlong STRING_TAG = 1;
    void load(JNIEnv *jni);
    void unload(JNIEnv *jni);
} registry;

#pragma pack(push, 1)
struct MainBufferHeader {
    jlong sequenceNum = 0;
    jint referenceNodeCount = 0;
    jint location = sizeof(MainBufferHeader);
    jint locals = -1;
    jint heap = -1;
    jint classes = -1;
};
#pragma pack(pop)

inline bool check(bool condition, std::source_location loc = std::source_location::current()) {
    if (!condition) [[unlikely]]
        std::fprintf(stderr, "Check failed at %s:%d\n", loc.function_name(), loc.line());
    return condition;
}

template <typename T>
bool ok(T err, int allowedErr = 0, std::source_location loc = std::source_location::current()) {
    if (err && err != allowedErr) { [[unlikely]]
        std::fprintf(stderr, "Error %d at %s:%d\n", err, loc.function_name(), loc.line());
        return false;
    }
    return true;
}

template <typename T>
T jniCatch(T value, JNIEnv *jni) {
    if (!value && jni->ExceptionCheck()) [[unlikely]]
        jni->ExceptionDescribe();
    return value;
}

template <typename T>
T replaceByGlobal(T localRef, JNIEnv *jni) {
    if (!localRef)
        return nullptr;
    T global = static_cast<T>(jni->NewGlobalRef(localRef));
    jni->DeleteLocalRef(localRef);
    return global;
}

template <typename T>
void dealloc(T *ptr) {
    ti->Deallocate(reinterpret_cast<unsigned char *>(ptr));
}

template <typename T>
class TiPtr {
public:
    TiPtr() = default;
    ~TiPtr() { dealloc(ptr); }
    operator T *() { return ptr; }
    T **operator &() { return &ptr; }

    TiPtr(TiPtr&& other) noexcept : ptr(std::exchange(other.ptr, nullptr)) {}
    TiPtr& operator=(TiPtr&& other) noexcept {
        std::swap(ptr, other.ptr);
        return *this;
    }
private:
    T *ptr = nullptr;
};

template <typename T>
class JniLocal {
public:
    JniLocal(T ref, JNIEnv *jni) : ref(ref), jni(jni) {};
    ~JniLocal() { jni->DeleteLocalRef(ref); }
    operator T() { return ref; }
private:
    T ref;
    JNIEnv *jni;
};


template <int Id = 0>
class ScopeTime {
    using Clock = std::chrono::steady_clock;
    std::chrono::time_point<Clock> start = Clock::now();
    inline static std::atomic<long long> total{0};
    inline static std::atomic<long long> count{0};
    inline static struct Reporter {
        ~Reporter() {
            std::fprintf(stderr, "C++ timer %d: %lld ms, %lldx\n", Id, total / 1'000'000, count.load());
        }
    } reportAtProgramExit;
public:
    ~ScopeTime() {
        auto end = Clock::now();
        auto diff = std::chrono::nanoseconds(end - start).count();
        total.fetch_add(diff, std::memory_order_relaxed);
        count.fetch_add(1, std::memory_order_relaxed);
        (void) reportAtProgramExit;
    }
};
