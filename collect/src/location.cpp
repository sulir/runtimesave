#include <algorithm>
#include <jni.h>

#include "agent.hpp"
#include "buffer.hpp"
#include "location.hpp"

static bool isObsolete(jmethodID method) {
    jboolean obsolete;
    if (!ok(ti->IsMethodObsolete(method, &obsolete)))
        return true;
    return obsolete;
}

static bool loadMethodInfo(jmethodID method) {
    if (method == methodInfo.method && !isObsolete(method))
        return true;

    methodInfo.cleanup();
    methodInfo = {};
    
    jclass klass;
    if (!ok(ti->GetMethodDeclaringClass(method, &klass)))
        return false;
    if (!ok(ti->GetClassSignature(klass, &methodInfo.classSig, nullptr)))
        return false;
    if (!ok(ti->GetMethodName(method, &methodInfo.name, &methodInfo.sig, nullptr)))
        return false;
    if (!ok(ti->GetLineNumberTable(method, &methodInfo.linesSize, &methodInfo.lines)))
        return false;
    if (!ok(ti->GetLocalVariableTable(method, &methodInfo.localsSize, &methodInfo.locals),
            JVMTI_ERROR_ABSENT_INFORMATION))
        return false;

    methodInfo.method = method;
    return true;
}

static int getLineNumber(jlocation location) {
    jvmtiLineNumberEntry *first = methodInfo.lines;
    auto entry = std::upper_bound(first, first + methodInfo.linesSize, location,
        [](jlocation value, const jvmtiLineNumberEntry& entry) {
            return value < entry.start_location;
        });
    
    if (entry == first)
        return -1;
    return (entry - 1)->line_number;
}

bool readLocation(jmethodID method, jlocation location, Buffer& buffer) {
    if (!loadMethodInfo(method))
        return false;

    int line = getLineNumber(location);
    if (line == - 1)
        return false;

    buffer.addString(methodInfo.classSig);
    buffer.addString(methodInfo.name);
    buffer.addString(methodInfo.sig);
    buffer.addInt(line);
    return true;
}
