#pragma once

#include <concepts>
#include <cstring>
#include <type_traits>
#include <jni.h>
#include <string>
#include <utility>

class Buffer {
public:
    Buffer() = default;
    Buffer(const Buffer&) = delete;
    Buffer& operator=(const Buffer&) = delete;

    void add(const void *data, size_t size);
    template <typename T> requires (!std::is_pointer_v<T>) void add(const T value) { add(&value, sizeof(T)); }
    void addString(const char *str);
    void addString(const std::string& str);
    template <typename T, typename... Args> void emplace(Args&&... args) {
        if (!grow(sizeof(T)))
            return;
        new (static_cast<std::byte *>(mem) + pos) T(std::forward<Args>(args)...);
        pos += sizeof(T);
    }
    size_t position();
    template <typename T> T *head() { return reinterpret_cast<T *>(mem); }
    void checkpoint();
    void restore();
    jobject result(JNIEnv *env);
    ~Buffer();
private:
    bool grow(size_t added);

    void *mem = nullptr;
    size_t capacity = 0;
    size_t pos = 0;
    size_t checkpointPos = 0;
};
