@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [FastGPU] Running Compute Demo (via JitPack)...
call mvn -U -f examples/Demo/pom.xml compile exec:java -Dexec.mainClass=fastgpu.MandelbrotDemo
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Demo failed.
    pause
    exit /b %ERRORLEVEL%
)
pause
