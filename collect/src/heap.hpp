#pragma once

#include <jni.h>
#include <vector>

#include "agent.hpp"
#include "buffer.hpp"

class ObjectClass {
public:
    static jclass get(JNIEnv *jni) {
        if (!cls)
            cls = replaceWithGlobal(jniCatch(jni->FindClass("java/lang/Object"), jni), jni);
        return cls;
    }
    static void dispose(JNIEnv *jni) {
        jni->DeleteGlobalRef(cls);
    }
private:
    static inline jclass cls = nullptr;
};

bool readObjects(const std::vector<jobject>& objects, Buffer& buffer, JNIEnv *jni);
