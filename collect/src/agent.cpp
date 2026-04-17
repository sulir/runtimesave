#include <array>
#include <jni.h>
#include <jvmti.h>

#include "agent.hpp"
#include "buffer.hpp"
#include "classes.hpp"
#include "heap.hpp"
#include "locals.hpp"
#include "location.hpp"

void Registry::load(JNIEnv *jni) {
    objectClass = replaceByGlobal(jniCatch(jni->FindClass("java/lang/Object"), jni), jni);

    jclass stringClass = jniCatch(jni->FindClass("java/lang/String"), jni);
    if (stringClass)
        ok(ti->SetTag(stringClass, STRING_TAG));
    
    jclass classClass = jniCatch(jni->FindClass("java/lang/Class"), jni);
    if (classClass)
        ok(ti->SetTag(classClass, CLASS_TAG));
}

void Registry::unload(JNIEnv *jni) {
    jni->DeleteGlobalRef(objectClass);
}

bool Registry::getBufferReader(jclass *klass, jmethodID *constructor, JNIEnv *jni) {
    if (!reader) {
        reader = replaceByGlobal(jniCatch(jni->FindClass("io/github/sulir/runtimesave/rt/BufferReader"), jni), jni);
        if (!reader)
            return false;
        readerInit = jniCatch(jni->GetMethodID(reader, "<init>", "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V"), jni);
        if (!readerInit)
            return false;
    }
    *klass = reader;
    *constructor = readerInit;
    return true;
}

void JNICALL onVMInit(jvmtiEnv *, JNIEnv *jni, jthread) {
    registry.load(jni);
}

void JNICALL onVMDeath(jvmtiEnv *, JNIEnv *jni) {
    registry.unload(jni);
}

void JNICALL onClassLoad(jvmtiEnv *, JNIEnv *jni, jthread, jclass klass) {
    jlong tag;
    if (ok(ti->GetTag(klass, &tag)) && tag != registry.STRING_TAG && tag != registry.CLASS_TAG)
        classCache.add(klass, jni);
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
    callbacks.ClassLoad = onClassLoad;
    if (!ok(ti->SetEventCallbacks(&callbacks, sizeof callbacks)))
        return 1;
    static constexpr std::array events = {JVMTI_EVENT_VM_INIT, JVMTI_EVENT_VM_DEATH, JVMTI_EVENT_CLASS_LOAD};
    for (jvmtiEvent event : events)
        if (!ok(ti->SetEventNotificationMode(JVMTI_ENABLE, event, nullptr)))
            return 1;
    
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL Java_io_github_sulir_runtimesave_rt_Collector_readData(JNIEnv *jni, jclass) {
    jclass bufferReader;
    jmethodID bufferReaderInit;
    if (!registry.getBufferReader(&bufferReader, &bufferReaderInit, jni))
        return nullptr;
    
    Buffer main;
    main.add(MainBufferHeader{});

    jmethodID method;
    jlocation location;
    MethodInfo& methodInfo = MethodInfo::getThreadInstance();
    if (!readLocation(&method, &location, methodInfo, main))
        return nullptr;
    
    main.head<MainBufferHeader>()->locals = main.position();
    std::vector<jobject> objects;
    if (!readLocals(location, methodInfo, main, objects))
        return nullptr;
    
    main.head<MainBufferHeader>()->heap = main.position();
    Buffer refNodes;
    HeapData heapData{main, refNodes};
    if (!readHeap(objects, heapData, jni))
        return nullptr;
    main.head<MainBufferHeader>()->sequenceNum = heapData.sequenceNum;
    main.head<MainBufferHeader>()->referenceNodeCount = heapData.referenceNodeCount;
    
    main.head<MainBufferHeader>()->classes = main.position();
    loadClassesInfo(heapData.cachedClasses, heapData.uncachedClasses, main, jni);

    return jniCatch(jni->NewObject(bufferReader, bufferReaderInit, main.result(jni), refNodes.result(jni)), jni);
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_sulir_runtimesave_rt_BufferReader_dispose(JNIEnv *jni, jclass, jobject buffer) {
    free(jni->GetDirectBufferAddress(buffer));
}
