@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
if errorlevel 1 (
    call "C:\Program Files (x86)\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
)
if errorlevel 1 (
    call "C:\Program Files (x86)\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
)
if errorlevel 1 (
    call "C:\Program Files (x86)\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
)

cl.exe /LD /EHsc /std:c++17 /I"C:\Program Files\Java\jdk-25\include" /I"C:\Program Files\Java\jdk-25\include\win32" /I"fastgpu-native\include" /I"fastgpu-native\include\vulkan" fastgpu-native\src\jni\fastgpu_jni.cpp fastgpu-native\src\vk\vk_context.cpp fastgpu-native\src\vk\vk_memory.cpp fastgpu-native\src\vk\vk_buffer.cpp fastgpu-native\src\vk\vk_image.cpp fastgpu-native\src\vk\vk_pipeline.cpp fastgpu-native\src\vk\vk_dispatch.cpp fastgpu-native\src\vk\volk.c /Fe:fastgpu.dll
