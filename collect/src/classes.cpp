#include <jvmti.h>
#include <string>
#include <vector>

#include "agent.hpp"
#include "buffer.hpp"
#include "classes.hpp"

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

static void findTagged(const jclass* all, jint allCount, jlong start, jint count, std::vector<jclass>& tagged) {
    ScopeTime _;
    jint found = 0;
    for (jint i = 0; i < allCount && found < count; i++) {
        jlong tag;
        if (!ok(ti->GetTag(all[i], &tag)))
            continue;

        if (tag >= start && tag < start + count) {
            tagged[tag - start] = all[i];
            found++;
        }
    }
    check(found == count);
}

void loadClassesInfo(jlong startTag, jint count, Buffer& buffer) {
    if (count == 0)
        return;

    std::vector<jclass> newClasses(count);
    jint allCount = 0;
    jclass *allClasses = nullptr;
    ok(ti->GetLoadedClasses(&allCount, &allClasses));
    findTagged(allClasses, allCount, startTag, count, newClasses);
    dealloc(allClasses);

    for (jint i = 0; i < count; i++)
        loadClassInfo(newClasses[i], startTag + i, buffer);
}
