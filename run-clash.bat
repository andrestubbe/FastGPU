@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [FastGPU] Running Clash Java demo...
call mvn -U -f examples/clashjava/pom.xml compile exec:java -Dexec.mainClass=fastgpu.ClashJavaDemo
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Clash demo failed.
    pause
    exit /b %ERRORLEVEL%
)
pause
