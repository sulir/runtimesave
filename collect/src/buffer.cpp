#include <bit>
#include <cstdlib>
#include <cstring>
#include <jni.h>

#include "agent.hpp"
#include "buffer.hpp"

void Buffer::add(const void *data, size_t size) {
    if (size == 0 || !grow(size))
        return;

    std::memcpy(static_cast<std::byte *>(mem) + pos, data, size);
    pos += size;
}

void Buffer::addString(const char *str) {
    jsize length = strlen(str);
    add(length);
    add(str, length);
}

void Buffer::checkpoint() {
    checkpointPos = pos;
}

void Buffer::restore() {
    pos = checkpointPos;
}

static_assert(std::endian::native == std::endian::little, "ByteBuffer is set to LE on Java side");

jobject Buffer::result(JNIEnv *env) {
    if (capacity == SIZE_MAX)
        return nullptr;
    jobject nioBuffer = jniCatch(env->NewDirectByteBuffer(mem, pos), env);
    if (nioBuffer)
        pos = 0;
    return nioBuffer;
}

Buffer::~Buffer() {
    if (pos != 0)
        free(mem);
}

bool Buffer::grow(size_t added) {
    if (pos + added <= capacity || capacity == SIZE_MAX)
        return true;

    constexpr size_t ROUND = 1024 * 1024;
    size_t newCapacity = (pos + added + ROUND - 1) / ROUND * ROUND;
    void *newMem = std::realloc(mem, newCapacity);
    
    if (!newMem) {
        free(mem);
        mem = nullptr;
        capacity = SIZE_MAX;
        return false;
    }

    mem = newMem;
    capacity = newCapacity;
    return true;
}
