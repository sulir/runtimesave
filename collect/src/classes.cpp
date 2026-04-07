#include "buffer.hpp"
#include "classes.hpp"

void loadClassInfo(jint classTag, Buffer& buffer) {
    buffer.add(classTag);
    buffer.addString("Unknown");
    buffer.add(static_cast<jint>(-1));
    buffer.add(static_cast<jint>(0));
}

void loadClassesInfo(const std::vector<jint>& classTags, Buffer& buffer) {
    for (jint tag : classTags)
        loadClassInfo(tag, buffer);
}
