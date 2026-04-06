#pragma once

#include <jni.h>
#include <vector>

#include "agent.hpp"
#include "buffer.hpp"

bool readObjects(const std::vector<jobject>& objects, Buffer& buffer, JNIEnv *jni);
