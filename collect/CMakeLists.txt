cmake_minimum_required(VERSION 3.28)
project(runtimesave_collect)

find_package(JNI REQUIRED)

add_library(runtimesave SHARED
    src/agent.cpp)

target_include_directories(runtimesave PRIVATE
    ${JNI_INCLUDE_DIRS})

set_target_properties(runtimesave PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../dist
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../dist)
