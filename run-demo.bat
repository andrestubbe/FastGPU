@echo off
set PATH=C:\VulkanSDK\1.4.350.0\Bin;%PATH%
echo === Compiling FastGPU Demos ===
javac -d out fastgpu\*.java ..\FastCore\src\main\java\fastcore\*.java ..\FastImage\src\main\java\fastimage\*.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)

mkdir out\native 2>nul
copy /y fastgpu.dll out\native\fastgpu.dll >nul
copy /y ..\FastImage\target\classes\native\fastimage.dll out\native\fastimage.dll >nul

echo.
echo === Running VectorAddDemo ===
java -cp out --enable-native-access=ALL-UNNAMED fastgpu.VectorAddDemo
if errorlevel 1 (
    echo Failed to run VectorAddDemo.
    pause
    exit /b 1
)

echo.
echo === Running ImageBlurDemo ===
java -cp out --enable-native-access=ALL-UNNAMED fastgpu.ImageBlurDemo
if errorlevel 1 (
    echo Failed to run ImageBlurDemo.
    pause
    exit /b 1
)

echo.
echo === Running FastImageGPU Benchmark ===
java -cp out --enable-native-access=ALL-UNNAMED fastgpu.FastImageGPUDemo
if errorlevel 1 (
    echo Failed to run FastImageGPUDemo.
    pause
    exit /b 1
)

echo.
pause
