@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ⚡ FastGPU JMH Performance Benchmark...
cd examples\Benchmark
call run-benchmark.bat
cd ..\..

