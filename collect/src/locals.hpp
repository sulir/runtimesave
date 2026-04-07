#pragma once

#include <jni.h>
#include <vector>

#include "buffer.hpp"
#include "location.hpp"

bool readLocals(jlocation location, MethodInfo& methodInfo, Buffer& buffer, std::vector<jobject>& objects);
