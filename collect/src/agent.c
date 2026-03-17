#include <jni.h>
#include <jvmti.h>
#include <stdio.h>

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    return JNI_OK;
}
