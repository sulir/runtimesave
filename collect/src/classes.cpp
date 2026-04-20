#include <algorithm>
#include <jvmti.h>
#include <string>
#include <unordered_set>

#include "agent.hpp"
#include "buffer.hpp"
#include "classes.hpp"

ClassCache::ClassCache() {
    classes.reserve(32 * 1024);
    classes.resize(FIRST_FREE);
}

bool ClassCache::load(JNIEnv *jni) {
    objectClass = replaceByGlobal(jniCatch(jni->FindClass("java/lang/Object"), jni), jni);
    jclass classClass = jniCatch(jni->FindClass("java/lang/Class"), jni);
    jclass stringClass = jniCatch(jni->FindClass("java/lang/String"), jni);
    
    std::lock_guard<std::mutex> lock(mtx);
    return objectClass && add(classClass, CLASS_TAG, jni) && add(stringClass, STRING_TAG, jni);
}

void ClassCache::unload(JNIEnv *jni) {
    jni->DeleteGlobalRef(objectClass);
}

jlong ClassCache::add(jclass klass, jlong tag, JNIEnv *jni) {
    if (!ok(ti->SetTag(klass, tag)))
        return 0;

    jweak weak = jni->NewWeakGlobalRef(klass);
    if (!check(weak))
        return 0;

    if (classes.size() <= static_cast<size_t>(tag))
        classes.resize(tag + 1);
    classes[tag].weakRef = weak;
    return tag;
}

jlong ClassCache::addIfAbsent(jclass klass, JNIEnv *jni) {
    std::lock_guard<std::mutex> lock(mtx);
    jlong oldTag;
    if (!ok(ti->GetTag(klass, &oldTag)))
        return 0;
    if (oldTag != 0 && classes.size() > static_cast<size_t>(oldTag) && classes[oldTag].weakRef)
        return oldTag;
    
    jlong newTag = (oldTag == 0) ? classes.size() : oldTag;
    return add(klass, newTag, jni);
}

bool ClassCache::addSafe(jlong *tag) {
    if (*tag == 0) {
        *tag = classes.size();
        classes.push_back(ClassState{nullptr, true});
        return true;
    } else {
        bool isNew = !classes[*tag].sent;
        classes[*tag].sent = true;
        return isNew;
    }
}

void ClassCache::addClassObjectOnly(jlong *tag) {
    if (*tag == 0) {
        *tag = classes.size();
        classes.push_back(ClassState{nullptr, false});
    }
}

jclass ClassCache::release(jlong tag, JNIEnv *jni) {
    std::lock_guard<std::mutex> lock(mtx);
    jweak weak = classes[tag].weakRef;
    if (!weak)
        return nullptr;
    
    jclass local = static_cast<jclass>(jni->NewLocalRef(weak));
    if (!check(local)) {
        classes[tag].weakRef = nullptr;
        return nullptr;
    }
    
    jni->DeleteWeakGlobalRef(weak);
    classes[tag].weakRef = nullptr;
    return local;
}

std::mutex& ClassCache::mutex() {
    return mtx;
}

static std::string classSignatureToName(const char *sig) {
    std::string result;
    result.reserve(strlen(sig));

    size_t arrayDim = 0;
    while (sig[arrayDim] == '[')
        arrayDim++;
    
    char kind = sig[arrayDim];
    if (kind == 'L') {   
        for (size_t i = arrayDim + 1; sig[i] != ';'; i++)
            result += sig[i] == '/' ? '.' : sig[i];
    } else if (kind >= 'B' && kind <= 'Z') {
        result += primitiveTypes[kind - 'B'].name;
    }

    for (size_t i = 0; i < arrayDim; i++)
        result += "[]";
    return result;
}

static std::vector<JniLocal<jclass>> getClassHierarchy(jclass klass, JNIEnv *jni) {
    std::vector<JniLocal<jclass>> result;
    result.reserve(2);

    for (jclass cls = klass; cls != nullptr; cls = jni->GetSuperclass(cls))
        result.push_back(JniLocal{cls, jni});

    std::reverse(result.begin(), result.end());
    return result;
}

static bool addDirectInterfaces(jclass type, std::vector<jclass>& result) {
    jint count;
    TiPtr<jclass> interfaces;
    if (!ok(ti->GetImplementedInterfaces(type, &count, &interfaces)))
        return false;
    
    result.reserve(result.size() + count);
    for (jint i = 0; i < count; i++)
        result.push_back(interfaces[i]);

    return true;
}

static jint getInterfacesFieldCount(const std::vector<JniLocal<jclass>>& classHierarchy, JNIEnv *jni) {
    std::vector<jclass> stack;
    std::unordered_set<jlong> visited;
    jint result = 0;

    for (const JniLocal<jclass>& klass : classHierarchy)
        if (!addDirectInterfaces(klass, stack))
            return -1;

    while (!stack.empty()) {
        JniLocal<jclass> interface{stack.back(), jni};
        stack.pop_back();

        jlong tag = classCache.addIfAbsent(interface, jni);
        if (tag == 0)
            return -1;
        if (!visited.insert(tag).second)
            continue;
        
        jint count;
        TiPtr<jfieldID> fields;
        if (!ok(ti->GetClassFields(interface, &count, &fields)))
            return -1;
        result += count;

        if (!addDirectInterfaces(interface, stack))
            return -1;
    }
    return result;
}

static bool getFieldNames(const std::vector<JniLocal<jclass>>& classHierarchy, std::vector<TiPtr<char>>& result) {
    for (const JniLocal<jclass>& klass : classHierarchy) {
        jint count;
        TiPtr<jfieldID> fields;
        if (!ok(ti->GetClassFields(klass, &count, &fields)))
            return false;
        
        for (jint i = 0; i < count; i++) {
            TiPtr<char> name;
            if (!ok(ti->GetFieldName(klass, fields[i], &name, nullptr, nullptr)))
                return false;
            result.push_back(std::move(name));
        }
    }
    return true;
}

static void addClassInfo(jclass klass, jlong tag, Buffer& buffer, JNIEnv *jni) {
    TiPtr<char> signature;
    if (!check(klass) || !ok(ti->GetClassSignature(klass, &signature, nullptr)))
        return;
    
    std::vector<JniLocal<jclass>> hierarchy = getClassHierarchy(klass, jni);
    jint fieldStartIndex = getInterfacesFieldCount(hierarchy, jni);
    if (fieldStartIndex == -1)
        return;
    std::vector<TiPtr<char>> fieldNames;
    if (!getFieldNames(hierarchy, fieldNames))
        return;

    check(tag >= std::numeric_limits<jint>::min() && tag <= std::numeric_limits<jint>::max());
    buffer.add(static_cast<jint>(tag));
    buffer.addString(classSignatureToName(signature));
    buffer.add(fieldStartIndex);
    buffer.add(static_cast<jint>(fieldNames.size()));
    for (TiPtr<char>& name : fieldNames)
        buffer.addString(name);
}

static void loadUncachedClasses(std::unordered_set<jlong>& tags, Buffer& buffer, JNIEnv *jni) {
    jint allCount;
    TiPtr<jclass> allClasses;
    if (!ok(ti->GetLoadedClasses(&allCount, &allClasses)))
        return;
    
    for (jint i = 0; i < allCount && !tags.empty(); i++) {
        JniLocal<jclass> klass{allClasses[i], jni};
        jlong tag = classCache.addIfAbsent(klass, jni);
        if (tag == 0)
            continue;

        bool found = tags.erase(tag);
        if (found)
            addClassInfo(klass, tag, buffer, jni);
    }
    check(tags.empty());
}

void loadClassesInfo(const std::vector<jlong>& newClasses, Buffer& buffer, JNIEnv *jni) {
    std::unordered_set<jlong> uncached{};

    for (jlong tag : newClasses) {
        JniLocal<jclass> klass{classCache.release(tag, jni), jni};
        if (klass)
            addClassInfo(klass, tag, buffer, jni);
        else
            uncached.insert(tag);
    }
    
    if (!uncached.empty())
        loadUncachedClasses(uncached, buffer, jni);
}
