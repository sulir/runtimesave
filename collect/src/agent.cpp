#include <array>
#include <jni.h>
#include <jvmti.h>

#include "agent.hpp"
#include "buffer.hpp"
#include "frame.hpp"
#include "heap.hpp"
#include "location.hpp"

void JNICALL onVMInit(jvmtiEnv *, JNIEnv *jni, jthread) {
    systemClasses.load(jni);
}

void JNICALL onVMDeath(jvmtiEnv *, JNIEnv *jni) {
    systemClasses.unload(jni);
}

extern "C" JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *, void *) {
    if(!ok(vm->GetEnv(reinterpret_cast<void **>(&ti), JVMTI_VERSION_21)))
        return 1;
    
    jvmtiCapabilities capabilities{};
    capabilities.can_get_line_numbers = 1;
    capabilities.can_access_local_variables = 1;
    capabilities.can_tag_objects = 1;
    if (!ok(ti->AddCapabilities(&capabilities)))
        return 1;

    jvmtiEventCallbacks callbacks{};
    callbacks.VMInit = onVMInit;
    callbacks.VMDeath = onVMDeath;
    if (!ok(ti->SetEventCallbacks(&callbacks, sizeof callbacks)))
        return 1;
    static constexpr std::array events = {JVMTI_EVENT_VM_INIT, JVMTI_EVENT_VM_DEATH};
    for (jvmtiEvent event : events)
        if (!ok(ti->SetEventNotificationMode(JVMTI_ENABLE, event, nullptr)))
            return 1;
    
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL Java_io_github_sulir_runtimesave_rt_Collector_readData(JNIEnv *env, jclass) {
    MethodInfo& methodInfo = MethodInfo::getThreadInstance();
    Buffer buffer;

    jmethodID method;
    jlocation location;
    if (!ok(ti->GetFrameLocation(nullptr, CALLER_DEPTH, &method, &location)))
        return nullptr;
    if (!readLocation(method, location, methodInfo, buffer))
        return nullptr;
    if (!readFrame(location, methodInfo, buffer, env))
        return nullptr;
    
    return buffer.result(env);
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_sulir_runtimesave_rt_BufferReader_dispose(JNIEnv *env, jclass, jobject buffer) {
    free(env->GetDirectBufferAddress(buffer));
}
