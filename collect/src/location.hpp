#pragma once

#include <jni.h>

class SourceLocation {
public:
    jclass cls(JNIEnv *env);
    jmethodID fromJvmTi(JNIEnv *env);
    void cleanup(JNIEnv *env);
private:
    jclass cls_ = nullptr;
    jmethodID fromJvmTi_ = nullptr;
};

extern SourceLocation sourceLocation;
