@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [FastGPU] Running Clash Java demo...
mvn -U -f examples/clashjava/pom.xml compile exec:java -Dexec.mainClass=fastgpu.ClashJavaDemo
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Clash Java demo failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [FastGPU] Running Clash GPU benchmark...
mvn -U -f examples/clashjava/pom.xml compile exec:java -Dexec.mainClass=fastgpu.ClashGPUBenchmark
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Clash GPU benchmark failed.
    pause
    exit /b %ERRORLEVEL%
)
pause
