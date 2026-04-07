#pragma once

#include <jni.h>
#include <vector>

#include "buffer.hpp"

void loadClassesInfo(const std::vector<jint>& classTags, Buffer& buffer);
