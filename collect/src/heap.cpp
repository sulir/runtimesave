#include <jni.h>

#include "heap.hpp"

static jobjectArray createRoot(const std::vector<jobject>& objects, JNIEnv *jni) {
    size_t count = objects.size();
    jobjectArray array = jni->NewObjectArray(count, systemClasses.objectClass, nullptr);
    if (!array)
        return nullptr;
    
    for (size_t i = 0; i < count ; i++)
        jni->SetObjectArrayElement(array, i, objects[i]);
    return array;
}

static jint referenceCallback(jvmtiHeapReferenceKind, const jvmtiHeapReferenceInfo *, jlong,
        jlong, jlong, jlong *, jlong *, jint, void *) {
    return JVMTI_VISIT_ABORT;
}

bool readObjects(const std::vector<jobject>& objects, Buffer& buffer, JNIEnv *jni) {
    jobjectArray root = createRoot(objects, jni);
    if (!root)
        return false;

    jvmtiHeapCallbacks callbacks{};
    callbacks.heap_reference_callback = referenceCallback;
    ti->FollowReferences(0, nullptr, root, &callbacks, nullptr);

    buffer.add(nullptr, 0);
    return true;
}
