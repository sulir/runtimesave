#pragma once

#include <concepts>
#include <cstring>
#include <type_traits>
#include <jni.h>

class Buffer {
public:
    static Buffer& getThreadInstance();
    void reset();
    void checkpoint();
    void restore();
    void add(const void *data, size_t size);
    template <typename T> requires (!std::is_pointer_v<T>)
    void add(T value) {
        constexpr size_t size = sizeof(T);
        if (!grow(size))
            return;

        std::memcpy(static_cast<std::byte *>(mem) + pos, &value, size);
        pos += size;
    }
    void addString(const char *str);
    jobject result(JNIEnv *env);
    ~Buffer();
private:
    bool grow(size_t added);

    void *mem = nullptr;
    size_t capacity = 0;
    size_t pos = 0;
    size_t checkpointPos = 0;
    bool failed = false;
};
