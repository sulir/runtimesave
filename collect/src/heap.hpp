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
struct HeapReference {
    jbyte kind;
    jlong from;
    jint index;
    jlong to;
};

struct ObjectNode {
    jbyte kind;
    jlong objectTag;
    jlong classTag;
};

struct ArrayNode {
    jbyte kind;
    jlong objectTag;
    jlong classTag;
    jint length;
};

struct StringNode {
    jbyte kind;
    jlong objectTag;
    jint length;
};

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
