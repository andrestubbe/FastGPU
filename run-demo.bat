@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [FastGPU] Running Compute Demo (via JitPack)...
call mvn -f examples/Demo/pom.xml compile exec:java -Dexec.mainClass=fastgpu.FastGPUDemo
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Demo failed.
    pause
    exit /b %ERRORLEVEL%
)
pause
