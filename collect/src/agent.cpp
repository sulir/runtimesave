#include <jni.h>
#include <jvmti.h>

#include "common.hpp"
#include "location.hpp"

jvmtiEnv *ti = nullptr;

extern "C" JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *, void *) {
    if(!jni_ok(vm->GetEnv(reinterpret_cast<void **>(&ti), JVMTI_VERSION_21)))
        return 1;
    
    jvmtiCapabilities capabilities{};
    capabilities.can_get_line_numbers = 1;
    if (!jvmti_ok(ti->AddCapabilities(&capabilities)))
        return 1;

    return 0;
}

extern "C" JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
    ti = nullptr;

    JNIEnv *jni;
    if(!jni_ok(vm->GetEnv(reinterpret_cast<void **>(&jni), JNI_VERSION_21)))
        return;
    sourceLocation.cleanup(jni);
}
