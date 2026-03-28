#pragma once

#include <jni.h>

#include "common.hpp"

class SourceLocation {
public:
    jclass cls(JNIEnv *env);
    jmethodID fromJvmTi(JNIEnv *env);
    void cleanup(JNIEnv *env);
private:
    jclass cls_ = nullptr;
    jmethodID fromJvmTi_ = nullptr;
};

inline SourceLocation sourceLocation;

struct MethodInfo {
    jmethodID method;
    char *classSig;
    char *name;
    char *sig;
    jint linesSize;
    jvmtiLineNumberEntry *lines;
    jint localsSize;
    jvmtiLocalVariableEntry *locals;

    void cleanup() {
        dealloc(classSig);
        dealloc(name);
        dealloc(sig);
        dealloc(lines);
        for (int i = 0; i < localsSize; i++) {
            dealloc(locals[i].name);
            dealloc(locals[i].signature);
            dealloc(locals[i].generic_signature);
        }
        dealloc(locals);
    }
    ~MethodInfo() { cleanup(); };
};

inline thread_local MethodInfo methodInfo = {};

jobject readLocation(JNIEnv *env, jmethodID method, jlocation location);
