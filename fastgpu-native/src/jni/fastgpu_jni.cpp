#include <jni.h>
#include <cstdint>
#include <vector>
#include <string>
#include <cstring>

// Simple CPU-only stub backend for v0.1
struct FastGPUContext {
    std::string backend;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeCreate(JNIEnv* env, jclass, jstring jbackend) {
    const char* backend = env->GetStringUTFChars(jbackend, nullptr);
    auto* ctx = new FastGPUContext();
    ctx->backend = backend;
    env->ReleaseStringUTFChars(jbackend, backend);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDestroy(JNIEnv*, jclass, jlong handle) {
    auto* ctx = reinterpret_cast<FastGPUContext*>(handle);
    delete ctx;
}

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeAllocBuffer(JNIEnv*, jclass, jlong, jlong sizeBytes) {
    // For v0.1 stub: just store a pointer to a heap buffer
    auto* data = new std::vector<std::uint8_t>(static_cast<size_t>(sizeBytes));
    return reinterpret_cast<jlong>(data);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeFreeBuffer(JNIEnv*, jclass, jlong, jlong bufferHandle) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(bufferHandle);
    delete data;
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeUploadFloats(JNIEnv* env, jclass, jlong, jlong bufferHandle,
                                            jfloatArray arr, jint len) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jfloat* src = env->GetFloatArrayElements(arr, &isCopy);
    std::memcpy(data->data(), src, static_cast<size_t>(len) * sizeof(float));
    env->ReleaseFloatArrayElements(arr, src, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeUploadBytes(JNIEnv* env, jclass, jlong, jlong bufferHandle,
                                           jbyteArray arr, jint len) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jbyte* src = env->GetByteArrayElements(arr, &isCopy);
    std::memcpy(data->data(), src, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(arr, src, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDownloadFloats(JNIEnv* env, jclass, jlong, jlong bufferHandle,
                                              jfloatArray arr, jint len) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jfloat* dst = env->GetFloatArrayElements(arr, &isCopy);
    std::memcpy(dst, data->data(), static_cast<size_t>(len) * sizeof(float));
    env->ReleaseFloatArrayElements(arr, dst, 0);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDownloadBytes(JNIEnv* env, jclass, jlong, jlong bufferHandle,
                                             jbyteArray arr, jint len) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jbyte* dst = env->GetByteArrayElements(arr, &isCopy);
    std::memcpy(dst, data->data(), static_cast<size_t>(len));
    env->ReleaseByteArrayElements(arr, dst, 0);
}

// Images + kernels + dispatch are stubs for v0.1

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeAllocImage(JNIEnv*, jclass, jlong, jint w, jint h, jstring) {
    auto* data = new std::vector<std::uint8_t>(static_cast<size_t>(w) * h * 4);
    return reinterpret_cast<jlong>(data);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeFreeImage(JNIEnv*, jclass, jlong, jlong imageHandle) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(imageHandle);
    delete data;
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeUploadImage(JNIEnv* env, jclass, jlong, jlong imageHandle,
                                           jobject buf, jint w, jint h, jstring) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(imageHandle);
    auto* src = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(buf));
    std::memcpy(data->data(), src, static_cast<size_t>(w) * h * 4);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDownloadImage(JNIEnv* env, jclass, jlong, jlong imageHandle,
                                             jobject buf, jint w, jint h, jstring) {
    auto* data = reinterpret_cast<std::vector<std::uint8_t>*>(imageHandle);
    auto* dst = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(buf));
    std::memcpy(dst, data->data(), static_cast<size_t>(w) * h * 4);
}

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeCompileKernel(JNIEnv*, jclass, jlong, jstring, jstring, jstring) {
    // v0.1 stub: just return non-zero handle
    return 1L;
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDestroyKernel(JNIEnv*, jclass, jlong) {
    // nothing for stub
}

JNIEXPORT jint JNICALL
Java_fastgpu_FastGPUImpl_nativeDispatchKernel(JNIEnv*, jclass, jlong, jlong,
                                              jint, jint, jint,
                                              jlongArray) {
    // v0.1 stub: no-op, pretend success
    return 0;
}

}
