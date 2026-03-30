#pragma once

#include <jni.h>

class Buffer {
public:
    static Buffer& getThreadInstance();
    void reset();
    void add(const void *data, size_t size);
    void addInt(int i);
    void addString(const char *str);
    jobject result(JNIEnv *env);
    ~Buffer();
private:
    bool grow(size_t added);

    void *mem = nullptr;
    size_t capacity = 0;
    size_t pos = 0;
    bool failed = false;
};
