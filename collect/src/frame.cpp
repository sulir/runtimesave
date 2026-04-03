#include <cstring>
#include <jvmti.h>
#include <vector>

#include "frame.hpp"
#include "heap.hpp"
#include "location.hpp"

template <typename T, typename Storage, typename Getter>
static jvmtiError readLocalPrimitive(Getter getter, int slot, jbyte kind, Buffer& buffer) {
    Storage value;
    jvmtiError err = (ti->*getter)(nullptr, CALLER_DEPTH, slot, &value);
    buffer.add(kind);
    buffer.add(static_cast<T>(value));
    return err;
}

static jvmtiError readLocalReference(int slot, Buffer& buffer, std::vector<jobject>& objects) {
    jobject value = nullptr;
    jvmtiError err = ti->GetLocalObject(nullptr, CALLER_DEPTH, slot, &value);
    if (value) {
        buffer.add('R');
        buffer.add(static_cast<jint>(objects.size()));
        objects.push_back(value);
    } else {
        buffer.add('N');
    }
    return err;
}

static jvmtiError readLocalValue(jbyte kind, int slot, Buffer& buffer, std::vector<jobject>& objects) {
    switch (kind) {
        case 'Z':
            return readLocalPrimitive<jboolean, jint>(&jvmtiEnv::GetLocalInt, slot, kind, buffer);
        case 'B':
            return readLocalPrimitive<jbyte, jint>(&jvmtiEnv::GetLocalInt, slot, kind, buffer);
        case 'C':
            return readLocalPrimitive<jchar, jint>(&jvmtiEnv::GetLocalInt, slot, kind, buffer);
        case 'S':
            return readLocalPrimitive<jshort, jint>(&jvmtiEnv::GetLocalInt, slot, kind, buffer);
        case 'I':
            return readLocalPrimitive<jint, jint>(&jvmtiEnv::GetLocalInt, slot, kind, buffer);
        case 'J':
            return readLocalPrimitive<jlong, jlong>(&jvmtiEnv::GetLocalLong, slot, kind, buffer);
        case 'F':
            return readLocalPrimitive<jfloat, jfloat>(&jvmtiEnv::GetLocalFloat, slot, kind, buffer);
        case 'D':
            return readLocalPrimitive<jdouble, jdouble>(&jvmtiEnv::GetLocalDouble, slot, kind, buffer);
        case 'L':
        case '[':
            return readLocalReference(slot, buffer, objects);
    }
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
}

bool readFrame(jlocation location, MethodInfo& methodInfo, Buffer& buffer, JNIEnv *jni) {
    std::vector<jobject> objects;
    objects.reserve(methodInfo.numLocals);
    buffer.checkpoint();

    for (int i = 0; i < methodInfo.numLocals; i++) {
        jvmtiLocalVariableEntry entry = methodInfo.locals[i];
        if (location < entry.start_location || location > entry.start_location + entry.length)
            continue;

        buffer.addString(entry.name);
        jbyte kind = entry.signature[0];

        jvmtiError err = readLocalValue(kind, entry.slot, buffer, objects);
        if (err == JVMTI_ERROR_TYPE_MISMATCH || err == JVMTI_ERROR_INVALID_SLOT) {
            buffer.restore();
            return true;
        } else if (!ok(err)) {
            return false;
        }
    }

    if (objects.empty())
        return true;
    else
        return readObjects(objects, buffer, jni);
}
