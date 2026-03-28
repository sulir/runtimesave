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
    capabilities.can_access_local_variables = 1;
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

extern "C" JNIEXPORT jobject JNICALL Java_io_github_sulir_runtimesave_rt_Collector_findLocation(JNIEnv *env, jclass) {
    constexpr int CALLER_STACK_POS = 3;
    jmethodID method;
    jlocation location;
    if (!jvmti_ok(ti->GetFrameLocation(nullptr, CALLER_STACK_POS, &method, &location)))
        return nullptr;

    return readLocation(env, method, location);
}
