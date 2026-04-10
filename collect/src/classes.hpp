#pragma once

#include <jni.h>

#include "buffer.hpp"

void loadClassesInfo(jlong startTag, jint count, Buffer& buffer);
