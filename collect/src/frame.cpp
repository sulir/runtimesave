#include <cstring>
#include <jvmti.h>
#include <vector>

#include "location.hpp"
#include "frame.hpp"

template <typename T, typename Storage, typename Getter>
static jvmtiError readLocalValueAs(Getter getter, int slot, Buffer& buffer) {
    Storage value;
    jvmtiError err = (ti->*getter)(nullptr, CALLER_DEPTH, slot, &value);
    buffer.add(static_cast<T>(value));
    return err;
}

static jvmtiError readLocalValue(char kind, int slot, Buffer& buffer, std::vector<jobject>& objects) {
    switch (kind) {
        case 'Z':
            return readLocalValueAs<jboolean, jint>(&jvmtiEnv::GetLocalInt, slot, buffer);
        case 'B':
            return readLocalValueAs<jbyte, jint>(&jvmtiEnv::GetLocalInt, slot, buffer);
        case 'C':
            return readLocalValueAs<jchar, jint>(&jvmtiEnv::GetLocalInt, slot, buffer);
        case 'S':
            return readLocalValueAs<jshort, jint>(&jvmtiEnv::GetLocalInt, slot, buffer);
        case 'I':
            return readLocalValueAs<jint, jint>(&jvmtiEnv::GetLocalInt, slot, buffer);
        case 'J':
            return readLocalValueAs<jlong, jlong>(&jvmtiEnv::GetLocalLong, slot, buffer);
        case 'F':
            return readLocalValueAs<jfloat, jfloat>(&jvmtiEnv::GetLocalFloat, slot, buffer);
        case 'D':
            return readLocalValueAs<jdouble, jdouble>(&jvmtiEnv::GetLocalDouble, slot, buffer);
        case 'L':
        case '[':
            jvmtiError err = ti->GetLocalObject(nullptr, CALLER_DEPTH, slot, &objects.emplace_back());
            buffer.add(static_cast<jint>(objects.size() - 1));
            return err;
    }
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
}

bool readFrame(JNIEnv *, jlocation location, MethodInfo& methodInfo, Buffer& buffer) {
    std::vector<jobject> objects;
    objects.reserve(methodInfo.numLocals);
    buffer.checkpoint();

    for (int i = 0; i < methodInfo.numLocals; i++) {
        jvmtiLocalVariableEntry entry = methodInfo.locals[i];
        if (location < entry.start_location || location > entry.start_location + entry.length)
            continue;

        buffer.addString(entry.name);
        jbyte kind = entry.signature[0];
        buffer.add(kind);

        jvmtiError err = readLocalValue(kind, entry.slot, buffer, objects);
        if (err == JVMTI_ERROR_TYPE_MISMATCH || err == JVMTI_ERROR_INVALID_SLOT) {
            buffer.restore();
            return true;
        } else if (!ok(err)) {
            return false;
        }
    }
    return true;
}
