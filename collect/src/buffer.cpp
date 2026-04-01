#include <bit>
#include <cstdlib>
#include <cstring>
#include <jni.h>

#include "agent.hpp"
#include "buffer.hpp"

Buffer& Buffer::getThreadInstance() {
    static thread_local Buffer instance;
    return instance;
}

void Buffer::reset() {
    pos = 0;
    failed = false;
}

void Buffer::checkpoint() {
    checkpointPos = pos;
}

void Buffer::restore() {
    pos = checkpointPos;
}

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

static_assert(std::endian::native == std::endian::little, "ByteBuffer is set to LE on Java side");

jobject Buffer::result(JNIEnv *env) {
    if (!mem || failed)
        return nullptr;
    return jniCatch(env->NewDirectByteBuffer(mem, pos), env);
}

Buffer::~Buffer() {
    free(mem);
}

bool Buffer::grow(size_t added) {
    if (pos + added <= capacity)
        return true;

    constexpr size_t ROUND = 1024 * 1024;
    capacity = (pos + added + ROUND - 1) / ROUND * ROUND;
    void *newMem = std::realloc(mem, capacity);
    
    if (!newMem) {
        failed = true;
        free(mem);
        mem = nullptr;
        return false;
    }

    mem = newMem;
    return true;
}
