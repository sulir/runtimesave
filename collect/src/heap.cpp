#include <atomic>
#include <jni.h>
#include <jvmti.h>
#include <mutex>

#include "agent.hpp"
#include "classes.hpp"
#include "heap.hpp"

static jobjectArray createRoot(const std::vector<jobject>& objects, JNIEnv *jni) {
    size_t count = objects.size();
    jobjectArray array = jni->NewObjectArray(count, classCache.objectClass, nullptr);
    if (!array)
        return nullptr;
    
    for (size_t i = 0; i < count ; i++)
        jni->SetObjectArrayElement(array, i, objects[i]);
    return array;
}

static void tagClass(jlong *tag, HeapData& data) {
    if (classCache.addSafe(tag))
        data.newClasses.push_back(*tag);
}

static void tagObject(jlong *tag, jlong classTag) {
    static jlong nextObjectTag = -1;

    if (classTag == classCache.CLASS_TAG)
        classCache.addClassObjectOnly(tag);
    else if (*tag == 0)
        *tag = nextObjectTag--;
}

static void addObjectOrArrayNode(jlong objectTag, jlong classTag, HeapData& data) {
    if (objectTag != 0) {
        data.buffer.emplace<ObjectOrArrayNode>(objectTag, static_cast<jint>(classTag));
        data.referenceNodeCount++;
    }
}

static jbyte getReferenceKind(jlong classTag, jint arrayLength) {
    if (classTag == classCache.STRING_TAG)
        return 'T';
    if (arrayLength != -1)
        return '[';
    return 'L';
}

static void addFieldEdge(jlong from, jlong fromCls, jint index, jlong *to, jlong toCls, jint arrLen, Buffer& buffer) {
    jbyte toKind = getReferenceKind(toCls, arrLen);
    tagObject(to, toCls);
    buffer.emplace<FieldEdge>(from, static_cast<jint>(fromCls), index, *to, toKind);
}

static void addElementEdge(jlong from, jint index, jlong *to, jlong toCls, jint arrLen, Buffer& buffer) {
    jbyte toKind = getReferenceKind(toCls, arrLen);
    tagObject(to, toCls);
    buffer.emplace<ElementEdge>(from, index, *to, toKind);
}

static jint referenceCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong classTag,
        jlong referrerClassTag, jlong, jlong *tag, jlong *referrerTag, jint arrLen, void *userData) {
    HeapData& data = *static_cast<HeapData *>(userData);

    switch (kind) {
        case JVMTI_HEAP_REFERENCE_CLASS:
            if (*tag != classCache.STRING_TAG) {
                tagClass(tag, data);
                addObjectOrArrayNode(*referrerTag, *tag, data);
            }
            return 0;
        case JVMTI_HEAP_REFERENCE_FIELD:
            if (referrerClassTag == classCache.STRING_TAG)
                return 0;
            addFieldEdge(*referrerTag, referrerClassTag, info->field.index, tag, classTag, arrLen, data.buffer);
            return JVMTI_VISIT_OBJECTS;
        case JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT:
            addElementEdge(*referrerTag, info->array.index, tag, classTag, arrLen, data.buffer);
            return JVMTI_VISIT_OBJECTS;
        default:
            return 0;
    }
}

static jint primitiveFieldCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong classTag,
        jlong *tagPtr, jvalue value, jvmtiPrimitiveType type, void *userData) {
    if (kind == JVMTI_HEAP_REFERENCE_STATIC_FIELD || classTag == classCache.STRING_TAG)
        return 0;
    Buffer& buffer = static_cast<HeapData *>(userData)->buffer;
    buffer.emplace<PrimitiveField>(type, *tagPtr, static_cast<jint>(classTag), info->field.index);
    buffer.add(&value, primitiveTypes[type - 'B'].size);
    return 0;
}

static jint primitiveArrayCallback(jlong, jlong, jlong *tagPtr, jint length, jvmtiPrimitiveType type,
        const void *elements, void *userData) {
    Buffer& buffer = static_cast<HeapData *>(userData)->buffer;
    buffer.emplace<PrimitiveArray>(static_cast<jbyte>(type + 'a' - 'A'), *tagPtr, length);
    buffer.add(elements, length * primitiveTypes[type - 'B'].size);
    return 0;
}

static jint stringCallback(jlong, jlong, jlong *tagPtr, const jchar *value, jint charCount, void *userData) {
    HeapData& data = *static_cast<HeapData *>(userData);
    data.buffer.emplace<StringNode>(*tagPtr, charCount);
    data.buffer.add(value, charCount * sizeof(jchar));
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
    
    std::lock_guard<std::mutex> lock(classCache.mutex());
    if (!ok(ti->FollowReferences(0, nullptr, root, &callbacks, &heapData)))
        return false;
    heapData.sequenceNum = nextSequenceNum.fetch_add(1, std::memory_order_relaxed);
    return true;
}
