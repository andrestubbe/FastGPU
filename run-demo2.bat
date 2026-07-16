@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [FastGPU] Running Fluid Demo 2 (via JitPack)...
call mvn -U -f examples/Demo2/pom.xml compile exec:java -Dexec.mainClass=fastgpu.FluidDemo
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Demo2 failed.
    pause
    exit /b %ERRORLEVEL%
)
pause
