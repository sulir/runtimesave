#pragma once

#include <jni.h>
#include "buffer.hpp"
#include "location.hpp"

bool readFrame(jlocation location, MethodInfo& methodInfo, Buffer& buffer, JNIEnv *jni);
