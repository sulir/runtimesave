#include <algorithm>
#include <classfile_constants.h>
#include <jvmti.h>
#include <string>
#include <unordered_map>

#include "agent.hpp"
#include "buffer.hpp"
#include "classes.hpp"

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

ClassCache::ClassCache() {
    classes.reserve(32 * 1024);
    classes.resize(FIRST_FREE);
}

bool ClassCache::load(JNIEnv *jni) {
    objectClass = replaceByGlobal(jniCatch(jni->FindClass("java/lang/Object"), jni), jni);
    jclass classClass = jniCatch(jni->FindClass("java/lang/Class"), jni);
    jclass stringClass = jniCatch(jni->FindClass("java/lang/String"), jni);

    std::vector<JniLocal<jclass>> primitiveClasses;
    primitiveClasses.reserve(PRIMITIVE_COUNT);
    for (PrimitiveType type : primitiveTypes) {
        if (!type.boxed)
            continue;
        JniLocal<jclass> boxed{jniCatch(jni->FindClass(type.boxed), jni), jni};
        if (!boxed)
            return false;
        jfieldID field = jniCatch(jni->GetStaticFieldID(boxed, "TYPE", "Ljava/lang/Class;"), jni);
        if (!field)
            return false;
        jclass primitive = static_cast<jclass>(jniCatch(jni->GetStaticObjectField(boxed, field), jni));
        if (!field)
            return false;
        primitiveClasses.push_back(JniLocal{primitive, jni});
    }

    std::lock_guard<std::mutex> lock(mtx);
    if (!objectClass || !add(classClass, CLASS_TAG, jni) || !add(stringClass, STRING_TAG, jni))
        return false;
    for (size_t i = 0; i < PRIMITIVE_COUNT; i++)
        add(primitiveClasses[i], STRING_TAG + 1 + i, jni);
    return true;
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

bool ClassCache::addSafe(jlong *tag, SendState minState) {
    if (*tag == 0) {
        *tag = classes.size();
        classes.push_back(WeakClassInfo{nullptr, minState});
        return true;
    } else {
        if (classes[*tag].state < minState) {
            classes[*tag].state = minState;
            return true;
        } else {
            return false;
        }
    }
}

ClassInfo ClassCache::get(jlong tag, JNIEnv *jni) {
    std::lock_guard<std::mutex> lock(mtx);
    ClassInfo info{nullptr, classes[tag].state};
    
    jweak weak = classes[tag].weakRef;
    if (!weak)
        return info;
    
    jclass local = static_cast<jclass>(jni->NewLocalRef(weak));
    if (!check(local))
        classes[tag].weakRef = nullptr;
    
    info.klass = local;
    return info;
}

void ClassCache::release(jlong tag, JNIEnv *jni) {
    std::lock_guard<std::mutex> lock(mtx);
    jni->DeleteWeakGlobalRef(classes[tag].weakRef);
    classes[tag].weakRef = nullptr;
}

std::mutex& ClassCache::mutex() {
    return mtx;
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
            jint modifiers;
            if (!ok(ti->GetFieldModifiers(klass, fields[i], &modifiers)))
                return false;
            if (modifiers & JVM_ACC_STATIC) {
                result.emplace_back();
                continue;
            }

            TiPtr<char> name;
            if (!ok(ti->GetFieldName(klass, fields[i], &name, nullptr, nullptr)))
                return false;
            result.push_back(std::move(name));
        }
    }
    return true;
}

static void addClassInfo(jlong tag, const ClassInfo& info, Buffer& buffer, JNIEnv *jni) {
    TiPtr<char> signature;
    if (!ok(ti->GetClassSignature(info.klass, &signature, nullptr)))
        return;
    
    jint fieldStartIndex = -1;
    std::vector<TiPtr<char>> fieldNames{};

    if (info.state == SendState::All) {
        std::vector<JniLocal<jclass>> hierarchy = getClassHierarchy(info.klass, jni);
        fieldStartIndex = getInterfacesFieldCount(hierarchy, jni);
        if (fieldStartIndex == -1)
            return;
        if (!getFieldNames(hierarchy, fieldNames))
            return;
        classCache.release(tag, jni);
    }

    buffer.add(static_cast<jint>(tag));
    buffer.addString(classSignatureToName(signature));
    buffer.add(fieldStartIndex);
    buffer.add(static_cast<jint>(fieldNames.size()));
    for (TiPtr<char>& name : fieldNames)
        buffer.addString(name);
}

static void loadUncachedClasses(std::unordered_map<jlong, ClassInfo>& classes, Buffer& buffer, JNIEnv *jni) {
    jint allCount;
    TiPtr<jclass> allClasses;
    if (!ok(ti->GetLoadedClasses(&allCount, &allClasses)))
        return;
    
    for (jint i = 0; i < allCount && !classes.empty(); i++) {
        JniLocal<jclass> klass{allClasses[i], jni};
        jlong tag = classCache.addIfAbsent(klass, jni);
        if (tag == 0)
            continue;

        auto node = classes.extract(tag);
        if (!node.empty())
            addClassInfo(tag, ClassInfo{klass, node.mapped().state}, buffer, jni);
    }
    check(classes.empty());
}

void loadClassesInfo(const std::vector<jlong>& newClasses, Buffer& buffer, JNIEnv *jni) {
    std::unordered_map<jlong, ClassInfo> uncached{};

    for (jlong tag : newClasses) {
        ClassInfo info = classCache.get(tag, jni);
        if (info.klass)
            addClassInfo(tag, info, buffer, jni);
        else
            uncached[tag] = info;
    }
    
    if (!uncached.empty())
        loadUncachedClasses(uncached, buffer, jni);
}
