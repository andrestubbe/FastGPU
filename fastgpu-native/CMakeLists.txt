cmake_minimum_required(VERSION 3.20)
project(fastgpu_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)

add_library(fastgpu SHARED
    src/jni/fastgpu_jni.cpp
)

find_package(JNI REQUIRED)
target_include_directories(fastgpu PRIVATE ${JNI_INCLUDE_DIRS})

# später: Vulkan / CUDA libs linken
