#include <algorithm>
#include <jni.h>

#include "common.hpp"
#include "location.hpp"

JvmSourceLocation jvmSourceLocation;

jclass JvmSourceLocation::cls(JNIEnv *env) {
    if (!cls_) {
        jclass localClass = env->FindClass("io/github/sulir/runtimesave/SourceLocation");
        if (!jni_check(localClass, env))
            return nullptr;
        cls_ = static_cast<jclass>(env->NewGlobalRef(localClass));
        env->DeleteLocalRef(localClass);
    }
    return cls_;
}

jmethodID JvmSourceLocation::fromJvmTi(JNIEnv *env) {
    if (!fromJvmTi_) {
        fromJvmTi_ = env->GetStaticMethodID(cls_, "fromJvmTi",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)"
            "Lio/github/sulir/runtimesave/SourceLocation;");
        jni_check(fromJvmTi_, env);
    }
    return fromJvmTi_;
}

static bool getMethodInfo(jmethodID method, char **classSig, char **methodName, char **methodSig) {
    jclass klass;
    if (!jvmti_ok(ti->GetMethodDeclaringClass(method, &klass)))
        return false;
    if (!jvmti_ok(ti->GetClassSignature(klass, classSig, nullptr)))
        return false;
    if (!jvmti_ok(ti->GetMethodName(method, methodName, methodSig, nullptr)))
        return false;
    return true;
}

static int getLineNumber(jmethodID method, jlocation location) {
    int entryCount;
    JvmtiDealloc<jvmtiLineNumberEntry> table;

    if (!jvmti_ok(ti->GetLineNumberTable(method, &entryCount, table.out())))
        return -1;
    
    jvmtiLineNumberEntry *first = table.get();
    auto entry = std::upper_bound(first, first + entryCount, location,
        [](jlocation value, const jvmtiLineNumberEntry& entry) {
            return value < entry.start_location;
        });
    
    if (entry == first)
        return -1;
    return (entry - 1)->line_number;
}

extern "C" JNIEXPORT jobject JNICALL Java_io_github_sulir_runtimesave_rt_Collector_findLocation(JNIEnv *env, jclass) {
    constexpr int CALLER_STACK_POS = 3;
    jmethodID method;
    jlocation location;
    if (!jvmti_ok(ti->GetFrameLocation(nullptr, CALLER_STACK_POS, &method, &location)))
        return nullptr;

    JvmtiDealloc<char> classSig;
    JvmtiDealloc<char> methodName;
    JvmtiDealloc<char> methodSig;
    if (!getMethodInfo(method, classSig.out(), methodName.out(), methodSig.out()))
        return nullptr;

    int line = getLineNumber(method, location);
    if (line == - 1)
        return nullptr;

    jclass cls = jvmSourceLocation.cls(env);
    jmethodID calledMethod = jvmSourceLocation.fromJvmTi(env);
    if (!cls || !calledMethod)
        return nullptr;
    return env->CallStaticObjectMethod(cls, calledMethod, env->NewStringUTF(classSig.get()),
        env->NewStringUTF(methodName.get()), env->NewStringUTF(methodSig.get()), line);
}
