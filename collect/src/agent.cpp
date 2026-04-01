#include <jni.h>
#include <jvmti.h>

#include "agent.hpp"
#include "frame.hpp"
#include "buffer.hpp"
#include "location.hpp"

extern "C" JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *, void *) {
    if(!ok(vm->GetEnv(reinterpret_cast<void **>(&ti), JVMTI_VERSION_21)))
        return 1;
    
    jvmtiCapabilities capabilities{};
    capabilities.can_get_line_numbers = 1;
    capabilities.can_access_local_variables = 1;
    if (!ok(ti->AddCapabilities(&capabilities)))
        return 1;

    return 0;
}

extern "C" JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *) {
    ti = nullptr;
}

extern "C" JNIEXPORT jobject JNICALL Java_io_github_sulir_runtimesave_rt_Collector_readData(JNIEnv *env, jclass) {
    MethodInfo& methodInfo = MethodInfo::getThreadInstance();
    Buffer& buffer = Buffer::getThreadInstance();
    buffer.reset();

    jmethodID method;
    jlocation location;
    if (!ok(ti->GetFrameLocation(nullptr, CALLER_DEPTH, &method, &location)))
        return nullptr;
    if (!readLocation(method, location, methodInfo, buffer))
        return nullptr;
    if (!readFrame(env, location, methodInfo, buffer))
        return nullptr;
    
    return buffer.result(env);
}
