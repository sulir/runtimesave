#pragma once

#include <jni.h>
#include <unordered_set>
#include <vector>

#include "buffer.hpp"

struct HeapData {
    Buffer& buffer;
    jlong sequenceNum = 0;
    jint referenceNodeCount = 0;
    std::vector<jweak> cachedClasses{};
    std::unordered_set<jlong> uncachedClasses{};
};

#pragma pack(push, 1)
struct ObjectOrArrayNode {
    jbyte kind = 'R';
    jlong objectTag;
    jint classTag;
    ObjectOrArrayNode(jlong objectTag, jint classTag)
        : objectTag(objectTag), classTag (classTag) {};
};

struct StringNode {
    jbyte kind = 'T';
    jlong objectTag;
    jint charCount;
    StringNode(jlong objectTag, jint charCount)
        : objectTag(objectTag), charCount(charCount) {};
};

struct FieldEdge {
    jbyte kind = 'M';
    jlong from;
    jint fromClass;
    jint fieldIndex;
    jlong to;
    jbyte toKind;
    FieldEdge(jlong from, jint fromClass, jint fieldIndex, jlong to, jbyte toKind)
        : from(from), fromClass(fromClass), fieldIndex(fieldIndex), to(to), toKind(toKind) {};
};

struct ElementEdge {
    jbyte kind = 'E';
    jlong from;
    jint index;
    jlong to;
    jbyte toKind;
    ElementEdge(jlong from, jint index, jlong to, jbyte toKind)
        : from(from), index(index), to(to), toKind(toKind) {};
};

struct PrimitiveField {
    jbyte type;
    jlong objectTag;
    jint classTag;
    jint fieldIndex;
};

struct PrimitiveArray {
    jbyte type;
    jlong objectTag;
    jint length;
};
#pragma pack(pop)

bool readHeap(const std::vector<jobject>& objects, HeapData& heapData, JNIEnv *jni);
