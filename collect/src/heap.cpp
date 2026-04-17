#include <atomic>
#include <jni.h>
#include <jvmti.h>
#include <mutex>

#include "agent.hpp"
#include "classes.hpp"
#include "heap.hpp"

static jobjectArray createRoot(const std::vector<jobject>& objects, JNIEnv *jni) {
    size_t count = objects.size();
    jobjectArray array = jni->NewObjectArray(count, registry.objectClass, nullptr);
    if (!array)
        return nullptr;
    
    for (size_t i = 0; i < count ; i++)
        jni->SetObjectArrayElement(array, i, objects[i]);
    return array;
}

static void tagClass(jlong *tagPtr, HeapData& data) {
    static jlong nextClassTag = registry.CLASS_TAG + 1;

    jlong tag = *tagPtr;
    if (classCache.contains(tag)) {
        jweak klass = classCache.get(tag);
        data.cachedClasses.push_back(klass);
        if (tag != registry.STRING_TAG && tag != registry.CLASS_TAG)
            *tagPtr = nextClassTag++;
    } else if (tag == 0) {
        if (tag != registry.STRING_TAG && tag != registry.CLASS_TAG)
            *tagPtr = nextClassTag++;
        data.uncachedClasses.insert(*tagPtr);
    }
}

static void tagObject(jlong *tagPtr, jlong classTag, HeapData& data) {
    static jlong nextObjectTag = -1;

    if (*tagPtr == 0) {
        if (classTag == registry.CLASS_TAG)
            tagClass(tagPtr, data);
        else
            *tagPtr = nextObjectTag--;
    }
}

static void addObjectOrArrayNode(jlong objectTag, jlong classTag, HeapData& data) {
    if (objectTag != 0) {
        data.refNodes.emplace<ObjectOrArrayNode>(objectTag, static_cast<jint>(classTag));
        data.referenceNodeCount++;
    }
}

static jint referenceCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong classTag,
        jlong referrerClassTag, jlong, jlong *tag, jlong *referrerTag, jint, void *userData) {
    HeapData& data = *static_cast<HeapData *>(userData);

    switch (kind) {
        case JVMTI_HEAP_REFERENCE_CLASS:
            if (*tag != registry.STRING_TAG) {
                tagClass(tag, data);
                addObjectOrArrayNode(*referrerTag, *tag, data);
            }
            return 0;
        case JVMTI_HEAP_REFERENCE_FIELD:
            if (referrerClassTag == registry.STRING_TAG)
                return 0;
            tagObject(tag, classTag, data);
            data.main.emplace<FieldEdge>(*referrerTag, static_cast<jint>(referrerClassTag), info->field.index, *tag);
            return JVMTI_VISIT_OBJECTS;
        case JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT:
            tagObject(tag, classTag, data);
            data.main.emplace<ElementEdge>(*referrerTag, info->array.index, *tag);
            return JVMTI_VISIT_OBJECTS;
        default:
            return 0;
    }
}

static jint primitiveFieldCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong classTag,
        jlong *tagPtr, jvalue value, jvmtiPrimitiveType type, void *userData) {
    if (kind == JVMTI_HEAP_REFERENCE_STATIC_FIELD || classTag == registry.STRING_TAG)
        return 0;
    Buffer& main = static_cast<HeapData *>(userData)->main;
    main.emplace<PrimitiveField>(type, *tagPtr, static_cast<jint>(classTag), info->field.index);
    main.add(&value, primitiveTypes[type - 'B'].size);
    return 0;
}

static jint primitiveArrayCallback(jlong, jlong, jlong *tagPtr, jint length, jvmtiPrimitiveType type,
        const void *elements, void *userData) {
    Buffer& main = static_cast<HeapData *>(userData)->main;
    main.emplace<PrimitiveArray>(static_cast<jbyte>(type + 'a' - 'A'), *tagPtr, length);
    main.add(elements, length * primitiveTypes[type - 'B'].size);
    return 0;
}

static jint stringCallback(jlong, jlong, jlong *tagPtr, const jchar *value, jint charCount, void *userData) {
    HeapData& data = *static_cast<HeapData *>(userData);
    data.refNodes.emplace<StringNode>(*tagPtr, charCount);
    data.refNodes.add(value, charCount * sizeof(jchar));
    data.referenceNodeCount++;
    return 0;
}

bool readHeap(const std::vector<jobject>& locals, HeapData& heapData, JNIEnv *jni) {
    static std::atomic<jlong> nextSequenceNum{1};

    if (locals.empty()) {
        heapData.sequenceNum = nextSequenceNum.fetch_add(1, std::memory_order_relaxed);
        return true;
    }
    
    jobjectArray root = createRoot(locals, jni);
    if (!root)
        return false;

    jvmtiHeapCallbacks callbacks{};
    callbacks.heap_reference_callback = referenceCallback;
    callbacks.primitive_field_callback = primitiveFieldCallback;
    callbacks.array_primitive_value_callback = primitiveArrayCallback;
    callbacks.string_primitive_value_callback = stringCallback;
    
    static std::mutex mtx;
    std::lock_guard<std::mutex> lock(mtx);
    if (!ok(ti->FollowReferences(0, nullptr, root, &callbacks, &heapData)))
        return false;
    heapData.sequenceNum = nextSequenceNum.fetch_add(1, std::memory_order_relaxed);
    return true;
}
