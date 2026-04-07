#include <jni.h>
#include <jvmti.h>

#include "agent.hpp"
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

static jint tagClass(jlong *tag, HeapData *data) {
    static jint nextClassTag = 1;

    if (*tag == 0) {
        jint newTag = nextClassTag++;
        *tag = static_cast<jint>(newTag);
        data->newClasses.push_back(newTag);
    }
    return 0;
}

static jint referenceCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *, jlong classTag,
        jlong, jlong, jlong *tag, jlong *, jint, void *userData) {
    HeapData *data = static_cast<HeapData *>(userData);

    if (kind == JVMTI_HEAP_REFERENCE_CLASS)
        return tagClass(tag, data);

    if ((kind == JVMTI_HEAP_REFERENCE_FIELD || kind == JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT)
            && classTag != systemClasses.STRING_TAG)
        return JVMTI_VISIT_OBJECTS;
    return 0;
}

bool readHeap(const std::vector<jobject>& locals, Buffer& buffer, std::vector<jint>& newClasses, JNIEnv *jni) {
    if (locals.empty())
        return true;
    
    jobjectArray root = createRoot(locals, jni);
    if (!root)
        return false;

    jvmtiHeapCallbacks callbacks{};
    callbacks.heap_reference_callback = referenceCallback;
    
    HeapData heapData{buffer, newClasses};
    return ok(ti->FollowReferences(0, nullptr, root, &callbacks, &heapData));
}
