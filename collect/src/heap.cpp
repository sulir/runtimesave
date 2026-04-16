#include <atomic>
#include <jni.h>
#include <jvmti.h>
#include <mutex>

#include "agent.hpp"
#include "classes.hpp"
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

static void tagClass(jlong *tagPtr, HeapData *data) {
    static jlong nextClassTag = systemClasses.STRING_TAG + 1;

    jlong tag = *tagPtr;
    if (classCache.contains(tag)) {
        jweak klass = classCache.get(tag);
        data->cachedClasses.push_back(klass);
        *tagPtr = nextClassTag++;
    } else if (tag == 0) {
        *tagPtr = nextClassTag++;
        data->uncachedClasses.insert(*tagPtr);
    }
}

static jlong tagObject(jlong *tagPtr) {
    static jlong nextObjectTag = -1;

    if (*tagPtr == 0)
        *tagPtr = nextObjectTag--;
    return *tagPtr;
}

static void addObjectOrArrayNode(jlong objectTag, jlong classTag, Buffer& buffer) {
    if (objectTag != 0)
        buffer.emplace<ObjectOrArrayNode>(objectTag, static_cast<jint>(classTag));
}

static jbyte getReferenceKind(jlong classTag, jint arrayLength) {
    if (classTag == systemClasses.STRING_TAG)
        return 'T';
    if (arrayLength != -1)
        return '[';
    return 'L';
}

static void addFieldEdge(jlong from, jlong fromClass, jint index, jlong *to, jlong toClass, jint arrLen, Buffer& buf) {
    jbyte toKind = getReferenceKind(toClass, arrLen);
    buf.emplace<FieldEdge>(from, static_cast<jint>(fromClass), index, tagObject(to), toKind);
}

static void addElementEdge(jlong from, jint index, jlong *to, jlong toClass, jint arrLen, Buffer& buf) {
    jbyte toKind = getReferenceKind(toClass, arrLen);
    buf.emplace<ElementEdge>(from, index, tagObject(to), toKind);
}

static jint referenceCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong classTag,
        jlong referrerClassTag, jlong, jlong *tag, jlong *referrerTag, jint arrLen, void *userData) {
    HeapData *data = static_cast<HeapData *>(userData);

    switch (kind) {
        case JVMTI_HEAP_REFERENCE_CLASS:
            if (*tag != systemClasses.STRING_TAG) {
                tagClass(tag, data);
                addObjectOrArrayNode(*referrerTag, *tag, data->buffer);
            }
            return 0;
        case JVMTI_HEAP_REFERENCE_FIELD:
            if (referrerClassTag == systemClasses.STRING_TAG)
                return 0;
            addFieldEdge(*referrerTag, referrerClassTag, info->field.index, tag, classTag, arrLen, data->buffer);
            return JVMTI_VISIT_OBJECTS;
        case JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT:
            addElementEdge(*referrerTag, info->field.index, tag, classTag, arrLen, data->buffer);
            return JVMTI_VISIT_OBJECTS;
        default:
            return 0;
    }
}

static jint primitiveFieldCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong classTag,
        jlong *tagPtr, jvalue value, jvmtiPrimitiveType type, void *userData) {
    if (kind == JVMTI_HEAP_REFERENCE_STATIC_FIELD || classTag == systemClasses.STRING_TAG)
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
    Buffer& buffer = static_cast<HeapData *>(userData)->buffer;
    buffer.emplace<StringNode>(*tagPtr, charCount);
    buffer.add(value, charCount * sizeof(jchar));
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
