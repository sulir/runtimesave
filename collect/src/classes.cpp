#include <jvmti.h>
#include <string>

#include "agent.hpp"
#include "buffer.hpp"
#include "classes.hpp"

ClassCache::ClassCache() {
    classes.reserve(16*1024);
}

void ClassCache::add(jclass klass, JNIEnv *jni) {
    jweak weakClass = jni->NewWeakGlobalRef(klass);
    if (!weakClass)
        return;
    
    std::lock_guard<std::mutex> lock(mtx);
    if (!ok(ti->SetTag(klass, nextTag)))
        return;
    size_t pos = nextTag++ - MIN_TAG;
    classes.resize(pos + 1);
    classes[pos] = weakClass;
}

jweak ClassCache::get(jlong tag) {
    return classes[tag - MIN_TAG];
}

static std::string replaceByClassName(char *signature) {
    std::string result;
    result.reserve(strlen(signature));

    size_t arrayDim = 0;
    while (signature[arrayDim] == '[')
        arrayDim++;
    
    char kind = signature[arrayDim];
    switch (kind) {
        case 'Z': result += "boolean"; break;
        case 'B': result += "byte"; break;
        case 'C': result += "char"; break;
        case 'S': result += "short"; break;
        case 'I': result += "int"; break;
        case 'J': result += "long"; break;
        case 'F': result += "float"; break;
        case 'D': result += "double"; break;
        case 'L': [[likely]]
            for (size_t i = arrayDim + 1; signature[i] != ';'; i++)
                result += signature[i] == '/' ? '.' : signature[i];
            break;
    }

    for (size_t i = 0; i < arrayDim; i++)
        result += "[]";

    dealloc(signature);
    return result;
}

static void loadClassInfo(jclass klass, jlong tag, Buffer& buffer) {
    char *signature;
    if (!klass || !ok(ti->GetClassSignature(klass, &signature, nullptr)))
        return;

    buffer.add(static_cast<jint>(tag));
    buffer.addString(replaceByClassName(signature));
    buffer.add(static_cast<jint>(-1));
    buffer.add(static_cast<jint>(0));
}

static void loadCached(const std::vector<jweak>& classes, Buffer& buffer, JNIEnv *jni) {
    for (jweak weak : classes) {
        jclass klass = static_cast<jclass>(jni->NewLocalRef(weak));
        if (!klass)
            continue;
        
        jlong tag;
        if (!ok(ti->GetTag(klass, &tag)))
            continue;

        loadClassInfo(klass, tag, buffer);
    }
}

static void loadUncached(std::unordered_set<jlong>& tags, Buffer& buffer, JNIEnv *jni) {
    jint allCount;
    jclass *allClasses;
    if (!ok(ti->GetLoadedClasses(&allCount, &allClasses)))
        return;
    
    for (jint i = 0; i < allCount && !tags.empty(); i++) {
        jlong tag;
        if (!ok(ti->GetTag(allClasses[i], &tag)))
            continue;
        if (tag == 0) {
            classCache.add(allClasses[i], jni);
            continue;
        }

        if (tags.erase(tag))
            loadClassInfo(allClasses[i], tag, buffer);
    }

    check(tags.empty());
    dealloc(allClasses);
}

void loadClassesInfo(const std::vector<jweak>& cached, std::unordered_set<jlong>& uncached, Buffer& buffer,
        JNIEnv *jni) {
    loadCached(cached, buffer, jni);
    if (!uncached.empty())
        loadUncached(uncached, buffer, jni);
}
