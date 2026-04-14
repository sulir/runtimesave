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
    jint length = strlen(str);
    add(length);
    add(str, length);
}

void Buffer::addString(const std::string& str) {
    add(static_cast<jint>(str.size()));
    add(str.c_str(), str.size());
}

size_t Buffer::position() {
    return pos;
}

void Buffer::checkpoint() {
    checkpointPos = pos;
}

void Buffer::restore() {
    pos = checkpointPos;
}

static_assert(std::endian::native == std::endian::little, "ByteBuffer is set to LE on Java side");

jobject Buffer::result(JNIEnv *jni) {
    if (capacity == SIZE_MAX)
        return nullptr;
    jobject nioBuffer = jniCatch(jni->NewDirectByteBuffer(mem, pos), jni);
    if (nioBuffer)
        pos = 0;
    return nioBuffer;
}

Buffer::~Buffer() {
    if (pos != 0)
        free(mem);
}

bool Buffer::grow(size_t added) {
    size_t needed = pos + added;
    if (needed <= capacity || capacity == SIZE_MAX)
        return true;

    if (needed <= MAX_EXP)
        capacity = std::bit_ceil(needed);
    else
        capacity = (needed + MAX_EXP - 1) / MAX_EXP * MAX_EXP;

    if (capacity < MIN_SIZE)
        capacity = MIN_SIZE;
    void *newMem = std::realloc(mem, capacity);
    
    if (!newMem) {
        free(mem);
        mem = nullptr;
        capacity = SIZE_MAX;
        return false;
    }

    mem = newMem;
    return true;
}
