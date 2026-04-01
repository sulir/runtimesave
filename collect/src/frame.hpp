#pragma once

#include <jni.h>
#include "buffer.hpp"
#include "location.hpp"

bool readFrame(JNIEnv *env, jlocation location, MethodInfo& methodInfo, Buffer& buffer);
