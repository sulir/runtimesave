#include <jni.h>
#include <jvmti.h>
#include <stdio.h>

static jvmtiEnv *ti = NULL;

extern "C" JNIEXPORT void JNICALL Java_com_github_sulir_runtimesave_rt_Collector_doCollect(JNIEnv *env, jclass cls) {

}

extern "C" JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    return vm->GetEnv((void **) &ti, JVMTI_VERSION_21);
}
