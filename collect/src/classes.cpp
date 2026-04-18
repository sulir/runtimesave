#include <algorithm>
#include <jvmti.h>
#include <string>
#include <unordered_set>

#include "agent.hpp"
#include "buffer.hpp"
#include "classes.hpp"

ClassCache::ClassCache() {
    classes.reserve(32*1024);
}

void ClassCache::load(JNIEnv *jni) {
    objectClass = replaceByGlobal(jniCatch(jni->FindClass("java/lang/Object"), jni), jni);

    jclass classClass = jniCatch(jni->FindClass("java/lang/Class"), jni);
    if (classClass)
        classTag = addUnused(classClass, jni);
    
    jclass stringClass = jniCatch(jni->FindClass("java/lang/String"), jni);
    if (stringClass)
        ok(ti->SetTag(stringClass, STRING_TAG));
}

void ClassCache::unload(JNIEnv *jni) {
    jni->DeleteGlobalRef(objectClass);
}

jlong ClassCache::addUnused(jclass klass, JNIEnv *jni) {
    jlong oldTag;
    std::lock_guard<std::mutex> lock(mtx);
    if (!ok(ti->GetTag(klass, &oldTag)))
        return 0;
    if (oldTag != 0)
        return oldTag;

    jweak weak = jni->NewWeakGlobalRef(klass);
    if (!check(weak != nullptr))
        return oldTag;

    jlong newTag = MIN_UNUSED + classes.size();
    if (!ok(ti->SetTag(klass, newTag)))
        return oldTag;

    classes.push_back(weak);
    return newTag;
}

jweak ClassCache::useIfUnused(jlong *tagPtr, jlong *usedClassesTag) {
    jlong tag = *tagPtr;
    if (tag < MIN_UNUSED)
        return nullptr;
    
    size_t index = static_cast<size_t>(tag - MIN_UNUSED);
    if (index >= classes.size() || !classes[index])
        return nullptr;

    if (tag != STRING_TAG)
        *tagPtr = (*usedClassesTag)++;
    if (tag == classTag)
        classTag = *tagPtr;

    jweak klass = classes[index];
    classes[index] = nullptr;
    return klass;
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

static std::vector<jclass> getClassHierarchy(jclass klass, JNIEnv *jni) {
    std::vector<jclass> result;
    result.reserve(2);

    for (jclass cls = klass; cls != nullptr; cls = jni->GetSuperclass(cls))
        result.push_back(cls);

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

static jint getInterfacesFieldCount(const std::vector<jclass>& classHierarchy, JNIEnv *jni) {
    std::vector<jclass> stack;
    std::unordered_set<jlong> visited;
    jint result = 0;

    for (jclass klass : classHierarchy)
        if (!addDirectInterfaces(klass, stack))
            return -1;

    while (!stack.empty()) {
        JniLocal<jclass> interface{stack.back(), jni};
        stack.pop_back();

        jlong tag;
        if (!ok(ti->GetTag(interface, &tag)))
            return -1;
        if (tag == 0)
            if ((tag = classCache.addUnused(interface, jni)) == 0)
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

static bool getFieldNames(const std::vector<jclass>& classHierarchy, std::vector<TiPtr<char>>& result) {
    for (jclass klass : classHierarchy) {
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
    if (!klass || !ok(ti->GetClassSignature(klass, &signature, nullptr)))
        return;
    
    std::vector<jclass> hierarchy = getClassHierarchy(klass, jni);
    jint fieldStartIndex = getInterfacesFieldCount(hierarchy, jni);
    std::vector<TiPtr<char>> fieldNames;
    if (!getFieldNames(hierarchy, fieldNames))
        return;
    for (jclass cls : hierarchy)
        jni->DeleteLocalRef(cls);

    check(tag >= std::numeric_limits<jint>::min() && tag <= std::numeric_limits<jint>::max());
    buffer.add(static_cast<jint>(tag));
    buffer.addString(classSignatureToName(signature));
    buffer.add(fieldStartIndex);
    buffer.add(static_cast<jint>(fieldNames.size()));
    for (TiPtr<char>& name : fieldNames)
        buffer.addString(name);
}

static void loadCached(const std::vector<jweak>& classes, Buffer& buffer, JNIEnv *jni) {
    for (jweak weak : classes) {
        JniLocal<jclass> klass{static_cast<jclass>(jni->NewLocalRef(weak)), jni};
        if (!check(klass))
            continue;
        
        jlong tag;
        if (!ok(ti->GetTag(klass, &tag)))
            continue;

        addClassInfo(klass, tag, buffer, jni);
        jni->DeleteWeakGlobalRef(weak);
    }
}

static void loadUncached(std::unordered_set<jlong>& tags, Buffer& buffer, JNIEnv *jni) {
    jint allCount;
    TiPtr<jclass> allClasses;
    if (!ok(ti->GetLoadedClasses(&allCount, &allClasses)))
        return;
    
    for (jint i = 0; i < allCount && !tags.empty(); i++) {
        JniLocal<jclass> klass{allClasses[i], jni};
        jlong tag;
        if (!ok(ti->GetTag(klass, &tag)))
            continue;
        if (tag == 0) {
            classCache.addUnused(klass, jni);
            continue;
        }

        if (tags.erase(tag))
            addClassInfo(klass, tag, buffer, jni);
    }
}

void loadClassesInfo(const std::vector<jweak>& cached, std::unordered_set<jlong>& uncached, Buffer& buffer,
        JNIEnv *jni) {
    loadCached(cached, buffer, jni);
    if (!uncached.empty())
        loadUncached(uncached, buffer, jni);
}
