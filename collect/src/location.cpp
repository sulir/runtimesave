#include <algorithm>
#include <jni.h>

#include "common.hpp"
#include "location.hpp"

#define LOCATION_CLASS "io/github/sulir/runtimesave/misc/SourceLocation"

jclass SourceLocation::cls(JNIEnv *env) {
    if (!cls_) {
        jclass localClass = env->FindClass(LOCATION_CLASS);
        if (!jniCatch(localClass, env))
            return nullptr;
        cls_ = static_cast<jclass>(env->NewGlobalRef(localClass));
        env->DeleteLocalRef(localClass);
    }
    return cls_;
}

jmethodID SourceLocation::fromJvmTi(JNIEnv *env) {
    if (!fromJvmTi_) {
        jclass klass = cls(env);
        if (!klass)
            return nullptr;
        fromJvmTi_ = env->GetStaticMethodID(klass, "fromJvmTi",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)"
            "L" LOCATION_CLASS ";");
        jniCatch(fromJvmTi_, env);
    }
    return fromJvmTi_;
}

void SourceLocation::cleanup(JNIEnv *env) {
    if (cls_)
        env->DeleteGlobalRef(cls_);
}

static bool isObsolete(jmethodID method) {
    jboolean obsolete;
    if (!ok(ti->IsMethodObsolete(method, &obsolete)))
        return true;
    return obsolete;
}

static bool loadMethodInfo(jmethodID method) {
    if (method == methodInfo.method && !isObsolete(method))
        return true;

    methodInfo.cleanup();
    methodInfo = {};
    
    jclass klass;
    if (!ok(ti->GetMethodDeclaringClass(method, &klass)))
        return false;
    if (!ok(ti->GetClassSignature(klass, &methodInfo.classSig, nullptr)))
        return false;
    if (!ok(ti->GetMethodName(method, &methodInfo.name, &methodInfo.sig, nullptr)))
        return false;
    if (!ok(ti->GetLineNumberTable(method, &methodInfo.linesSize, &methodInfo.lines)))
        return false;
    if (!ok(ti->GetLocalVariableTable(method, &methodInfo.localsSize, &methodInfo.locals),
                  JVMTI_ERROR_ABSENT_INFORMATION))
        return false;

    methodInfo.method = method;
    return true;
}

static int getLineNumber(jlocation location) {
    jvmtiLineNumberEntry *first = methodInfo.lines;
    auto entry = std::upper_bound(first, first + methodInfo.linesSize, location,
        [](jlocation value, const jvmtiLineNumberEntry& entry) {
            return value < entry.start_location;
        });
    
    if (entry == first)
        return -1;
    return (entry - 1)->line_number;
}

jobject readLocation(JNIEnv *env, jmethodID method, jlocation location) {
    if (!loadMethodInfo(method))
        return nullptr;

    int line = getLineNumber(location);
    if (line == - 1)
        return nullptr;

    jclass cls = sourceLocation.cls(env);
    jmethodID calledMethod = sourceLocation.fromJvmTi(env);
    if (!cls || !calledMethod)
        return nullptr;
    return env->CallStaticObjectMethod(cls, calledMethod, env->NewStringUTF(methodInfo.classSig),
        env->NewStringUTF(methodInfo.name), env->NewStringUTF(methodInfo.sig), line);
}
