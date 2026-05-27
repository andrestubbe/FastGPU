@echo off
set PATH=C:\VulkanSDK\1.4.350.0\Bin;%PATH%
echo === Compiling Mandelbrot Demo ===
javac -d out fastgpu\*.java ..\FastCore\src\main\java\fastcore\*.java ..\FastImage\src\main\java\fastimage\*.java ..\FastTheme\src\main\java\fasttheme\*.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)

mkdir out\native 2>nul
copy /y fastgpu.dll out\native\fastgpu.dll >nul
copy /y ..\FastImage\target\classes\native\fastimage.dll out\native\fastimage.dll >nul
copy /y ..\FastTheme\build\fasttheme.dll out\native\fasttheme.dll >nul

echo.
echo === Running Mandelbrot GPU Zoomer ===
java -cp out --enable-native-access=ALL-UNNAMED fastgpu.MandelbrotDemo
if errorlevel 1 (
    echo Failed to run MandelbrotDemo.
    pause
    exit /b 1
)

echo.
pause
