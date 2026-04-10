#include <jni.h>
#include <jvmti.h>
#include <mutex>

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
    static jlong nextClassTag = 1;

    if (*tag == 0) {
        *tag = nextClassTag++;
        if (data->newClassesCount == 0)
            data->newClassesStart = *tag;
        data->newClassesCount++;
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

bool readHeap(const std::vector<jobject>& locals, HeapData& heapData, JNIEnv *jni) {
    if (locals.empty()) {
        heapData.sequenceNum = nextSequenceNum.fetch_add(1, std::memory_order_relaxed);
        return true;
    }
    
    jobjectArray root = createRoot(locals, jni);
    if (!root)
        return false;

    jvmtiHeapCallbacks callbacks{};
    callbacks.heap_reference_callback = referenceCallback;
    
    static std::mutex mtx;
    std::lock_guard<std::mutex> lock(mtx);
    if (!ok(ti->FollowReferences(0, nullptr, root, &callbacks, &heapData)))
        return false;
    heapData.sequenceNum = nextSequenceNum.fetch_add(1, std::memory_order_relaxed);
    return true;
}
