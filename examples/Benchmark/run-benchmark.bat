@echo off
echo ==================================================
echo ⚡ FastGPU JMH Performance Benchmark
echo ==================================================
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven build failed!
    exit /b %ERRORLEVEL%
)
echo.
echo Launching JMH Benchmark Runner...
java --enable-native-access=ALL-UNNAMED -jar target/benchmarks.jar
pause
