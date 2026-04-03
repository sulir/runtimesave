#include <jni.h>

#include "heap.hpp"

jobjectArray createRoot(const std::vector<jobject>& objects, JNIEnv *jni) {
    size_t count = objects.size();
    jobjectArray array = jni->NewObjectArray(count, ObjectClass::get(jni), nullptr);
    if (!array)
        return nullptr;
    
    for (size_t i = 0; i < count ; i++)
        jni->SetObjectArrayElement(array, i, objects[i]);
    return array;
}

bool readObjects(const std::vector<jobject>& objects, Buffer& buffer, JNIEnv *jni) {
    jobjectArray root = createRoot(objects, jni);
    if (!root)
        return false;

    buffer.add(nullptr, 0);
    return true;
}
