cmake_minimum_required(VERSION 3.28)
project(runtimesave_collect)

find_package(JNI REQUIRED)

add_library(runtimesave SHARED
    src/agent.cpp
    src/buffer.cpp
    src/frame.cpp
    src/heap.cpp
    src/location.cpp)

target_include_directories(runtimesave PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/src)

set_target_properties(runtimesave PROPERTIES
    CXX_STANDARD 20
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../dist
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../dist)

target_compile_options(runtimesave PRIVATE
    $<$<CXX_COMPILER_ID:GNU,Clang>:-Wall -Wextra -Wpedantic>
    $<$<CXX_COMPILER_ID:MSVC>:/W4 /permissive->)
