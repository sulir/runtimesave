#pragma once

#include <jni.h>
#include <jvmti.h>
#include <source_location>

extern jvmtiEnv *ti;

inline bool jvmti_ok(jvmtiError err, std::source_location loc = std::source_location::current()) {
    if (err != JVMTI_ERROR_NONE) {
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

template <typename T>
class JvmtiDealloc {
public:
    explicit JvmtiDealloc(): ptr(nullptr) {}
    ~JvmtiDealloc() {
        if (ptr)
            ti->Deallocate(reinterpret_cast<unsigned char *>(ptr));
    }

    T **out() { return &ptr; }
    T *get() { return ptr; }

    JvmtiDealloc(const JvmtiDealloc&) = delete;
    JvmtiDealloc& operator=(const JvmtiDealloc&) = delete;
private:
    T *ptr;
};
