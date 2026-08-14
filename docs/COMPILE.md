# FastGPU Compilation Guide

## Requirements

- **Windows 10/11**: Visual Studio 2022/2026 Developer Command Prompt (`MSVC C++17`), Vulkan SDK.
- **macOS**: Xcode Command Line Tools (`xcode-select --install`), Metal SDK.
- **Java**: JDK 17+ (Java 21 recommended).

---

## Build Steps (Windows)

```cmd
cd native
compile.bat
```

Produces `build/fastgpu.dll` and copies it to `lib/` and target directories.

---

## Build Steps (macOS)

```bash
cd native
./compile.sh
```

Produces `build/libfastgpu.dylib`.

---

## Maven Packaging

```bash
mvn clean install -DskipTests
```
