@echo off
echo === Compiling FastGPU Demos ===
javac -d out fastgpu\*.java ..\FastCore\src\main\java\fastcore\*.java ..\FastImage\src\main\java\fastimage\*.java
if errorlevel 1 (
    echo.
    echo Compilation failed!
    pause
    exit /b 1
)

REM Das JNI-DLL-File muss in den Classpath von FastCore kopiert werden
if not exist out\native mkdir out\native
if exist fastgpu.dll (
    copy /Y fastgpu.dll out\native\fastgpu.dll >nul
) else (
    echo Warnung: fastgpu.dll nicht im Stammverzeichnis gefunden. Bitte erst compile_native.bat ausfuehren.
)

echo.
echo === Running VectorAddDemo ===
java -cp out fastgpu.VectorAddDemo

echo.
echo === Running ImageBlurDemo ===
java -cp out fastgpu.ImageBlurDemo

echo.
pause
