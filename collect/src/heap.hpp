#pragma once

#include <atomic>
#include <jni.h>
#include <unordered_set>
#include <vector>

#include "buffer.hpp"

struct HeapData {
    Buffer& buffer;
    jlong sequenceNum = 0;
    std::vector<jweak> cachedClasses{};
    std::unordered_set<jlong> uncachedClasses{};
};

#pragma pack(push, 1)
struct ObjectOrArrayNode {
    jbyte kind;
    jlong objectTag;
    jlong classTag;
};

struct StringNode {
    jbyte kind;
    jlong objectTag;
    jint length;
};

struct ReferenceEdge {
    jbyte kind;
    jlong from;
    jint index;
    jlong to;
};
struct FieldEdge : ReferenceEdge {};
struct ElementEdge : ReferenceEdge {};

struct PrimitiveField {
    jbyte type;
    jlong objectTag;
    jint index;
    jvalue value;
};

struct PrimitiveArray {
    jbyte type;
    jlong objectTag;
    jint length;
};
#pragma pack(pop)

bool readHeap(const std::vector<jobject>& objects, HeapData& heapData, JNIEnv *jni);
