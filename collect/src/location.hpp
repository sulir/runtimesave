#pragma once

#include <jni.h>

#include "agent.hpp"
#include "buffer.hpp"

struct MethodInfo {
    jmethodID method;
    char *classSig;
    char *name;
    char *sig;
    jint numLines;
    jvmtiLineNumberEntry *lines;
    jint numLocals;
    jvmtiLocalVariableEntry *locals;

    static MethodInfo& getThreadInstance() {
        static thread_local MethodInfo instance = {};
        return instance;
    }
    
    void cleanup() {
        dealloc(classSig);
        dealloc(name);
        dealloc(sig);
        dealloc(lines);
        for (int i = 0; i < numLocals; i++) {
            dealloc(locals[i].name);
            dealloc(locals[i].signature);
            dealloc(locals[i].generic_signature);
        }
        dealloc(locals);
    }
    ~MethodInfo() { cleanup(); };
};

bool readLocation(jmethodID method, jlocation location, MethodInfo& methodInfo, Buffer& buffer);
