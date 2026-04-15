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
    if (classTag != systemClasses.STRING_TAG)
        buffer.emplace<ObjectOrArrayNode>('R', objectTag, classTag);
}

static jint addFieldEdge(jint index, jlong *tagPtr, jlong *referrerTagPtr, jlong classTag, Buffer& buffer) {
    buffer.emplace<FieldEdge>('M', *referrerTagPtr, index, tagObject(tagPtr));
    return classTag == systemClasses.STRING_TAG ? 0 : JVMTI_VISIT_OBJECTS;
}

static jint addElementEdge(jint index, jlong *tagPtr, jlong *referrerTagPtr, jlong classTag, Buffer& buffer) {
    buffer.emplace<ElementEdge>('E', *referrerTagPtr, index, tagObject(tagPtr));
    return classTag == systemClasses.STRING_TAG ? 0 : JVMTI_VISIT_OBJECTS;
}

static jint referenceCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong classTag, jlong,
        jlong, jlong *tagPtr, jlong *referrerTagPtr, jint, void *userData) {
    HeapData *data = static_cast<HeapData *>(userData);

    switch (kind) {
        case JVMTI_HEAP_REFERENCE_CLASS:
            tagClass(tagPtr, data);
            addObjectOrArrayNode(*referrerTagPtr, *tagPtr, data->buffer);
            return 0;
        case JVMTI_HEAP_REFERENCE_FIELD:
            return addFieldEdge(info->field.index, tagPtr, referrerTagPtr, classTag, data->buffer);
        case JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT:
            return addElementEdge(info->array.index, tagPtr, referrerTagPtr, classTag, data->buffer);
        default:
            return 0;
    }
}

static jint primitiveFieldCallback(jvmtiHeapReferenceKind kind, const jvmtiHeapReferenceInfo *info, jlong,
        jlong *tagPtr, jvalue value, jvmtiPrimitiveType type, void *userData) {
    if (kind == JVMTI_HEAP_REFERENCE_STATIC_FIELD)
        return 0;
    Buffer& buffer = static_cast<HeapData *>(userData)->buffer;
    buffer.emplace<PrimitiveField>(type, *tagPtr, info->field.index, value);
    return 0;
}

static jint primitiveArrayCallback(jlong, jlong, jlong *tagPtr, jint length, jvmtiPrimitiveType type,
        const void *elements, void *userData) {
    Buffer& buffer = static_cast<HeapData *>(userData)->buffer;
    buffer.emplace<PrimitiveArray>(static_cast<jbyte>(type + 'a' - 'A'), *tagPtr, length);
    buffer.add(elements, length * primitiveTypes[type - 'B'].size);
    return 0;
}

static jint stringCallback(jlong, jlong, jlong *tagPtr, const jchar *value, jint length, void *userData) {
    Buffer& buffer = static_cast<HeapData *>(userData)->buffer;
    buffer.emplace<StringNode>('S', *tagPtr, length);
    buffer.add(value, length * sizeof(jchar));
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
