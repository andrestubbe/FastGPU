#include <jni.h>
#include <cstdint>
#include <string>
#include <iostream>
#include <vector>
#include "../vk/vk_context.h"
#include "../vk/vk_buffer.h"
#include "../vk/vk_image.h"
#include "../vk/vk_pipeline.h"
#include "../vk/vk_dispatch.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeCreate(JNIEnv* env, jclass, jstring jbackend) {
    auto* ctx = new vk::VulkanContext();
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDestroy(JNIEnv*, jclass, jlong handle) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    delete ctx;
}

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeAllocBuffer(JNIEnv*, jclass, jlong handle, jlong sizeBytes) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* buffer = new vk::Buffer();
    *buffer = vk::createBuffer(*ctx, sizeBytes, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
    return reinterpret_cast<jlong>(buffer);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeFreeBuffer(JNIEnv*, jclass, jlong handle, jlong bufferHandle) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* buffer = reinterpret_cast<vk::Buffer*>(bufferHandle);
    vk::destroyBuffer(*ctx, *buffer);
    delete buffer;
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeUploadFloats(JNIEnv* env, jclass, jlong handle, jlong bufferHandle,
                                            jfloatArray arr, jint len) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* buffer = reinterpret_cast<vk::Buffer*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jfloat* src = env->GetFloatArrayElements(arr, &isCopy);
    vk::uploadToBuffer(*ctx, *buffer, src, static_cast<size_t>(len) * sizeof(float));
    env->ReleaseFloatArrayElements(arr, src, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeUploadBytes(JNIEnv* env, jclass, jlong handle, jlong bufferHandle,
                                           jbyteArray arr, jint len) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* buffer = reinterpret_cast<vk::Buffer*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jbyte* src = env->GetByteArrayElements(arr, &isCopy);
    vk::uploadToBuffer(*ctx, *buffer, src, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(arr, src, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDownloadFloats(JNIEnv* env, jclass, jlong handle, jlong bufferHandle,
                                              jfloatArray arr, jint len) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* buffer = reinterpret_cast<vk::Buffer*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jfloat* dst = env->GetFloatArrayElements(arr, &isCopy);
    vk::downloadFromBuffer(*ctx, *buffer, dst, static_cast<size_t>(len) * sizeof(float));
    env->ReleaseFloatArrayElements(arr, dst, 0);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDownloadBytes(JNIEnv* env, jclass, jlong handle, jlong bufferHandle,
                                             jbyteArray arr, jint len) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* buffer = reinterpret_cast<vk::Buffer*>(bufferHandle);
    jboolean isCopy = JNI_FALSE;
    jbyte* dst = env->GetByteArrayElements(arr, &isCopy);
    vk::downloadFromBuffer(*ctx, *buffer, dst, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(arr, dst, 0);
}

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeAllocImage(JNIEnv*, jclass, jlong handle, jint w, jint h, jstring jfmt) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* image = new vk::Image();
    VkFormat format = VK_FORMAT_R8G8B8A8_UNORM;
    *image = vk::createImage(*ctx, w, h, format);
    return reinterpret_cast<jlong>(image);
}

JNIEXPORT jbyteArray JNICALL Java_fastgpu_FastGPUImage_nativeDownload(JNIEnv *env, jobject, jlong ctxHandle, jlong imageHandle) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(ctxHandle);
    auto* image = reinterpret_cast<vk::Image*>(imageHandle);

    jbyteArray result = env->NewByteArray(image->size);
    void* data = env->GetPrimitiveArrayCritical(result, nullptr);
    try {
        vk::downloadFromImage(*ctx, *image, data, image->size);
    } catch (const std::exception& e) {
        std::cerr << "[FastGPU Native] Download Exception: " << e.what() << std::endl;
    }
    env->ReleasePrimitiveArrayCritical(result, data, 0);

    return result;
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeFreeImage(JNIEnv*, jclass, jlong handle, jlong imageHandle) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* image = reinterpret_cast<vk::Image*>(imageHandle);
    vk::destroyImage(*ctx, *image);
    delete image;
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeUploadImage(JNIEnv* env, jclass, jlong handle, jlong imageHandle,
                                           jobject buf, jint w, jint h, jstring) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* image = reinterpret_cast<vk::Image*>(imageHandle);
    auto* src = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(buf));
    vk::uploadToImage(*ctx, *image, src, static_cast<size_t>(w) * h * 4);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDownloadImage(JNIEnv* env, jclass, jlong handle, jlong imageHandle,
                                             jobject buf, jint w, jint h, jstring) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* image = reinterpret_cast<vk::Image*>(imageHandle);
    auto* dst = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(buf));
    vk::downloadFromImage(*ctx, *image, dst, static_cast<size_t>(w) * h * 4);
}

JNIEXPORT jlong JNICALL
Java_fastgpu_FastGPUImpl_nativeCompileKernel(JNIEnv* env, jclass, jlong handle, jstring jname, jstring jsource, jstring jlang) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    const char* source = env->GetStringUTFChars(jsource, nullptr);
    const char* lang = env->GetStringUTFChars(jlang, nullptr);
    
    auto* pipeline = new vk::Pipeline();
    *pipeline = vk::createPipeline(*ctx, source, lang);
    
    env->ReleaseStringUTFChars(jsource, source);
    env->ReleaseStringUTFChars(jlang, lang);
    return reinterpret_cast<jlong>(pipeline);
}

JNIEXPORT void JNICALL
Java_fastgpu_FastGPUImpl_nativeDestroyKernel(JNIEnv*, jclass, jlong handle) {
    auto* pipeline = reinterpret_cast<vk::Pipeline*>(handle);
    // Destroy happens in context or wrapper, but we leak ctx here since we don't pass it.
    // For v0.2, just delete the pointer. vk_pipeline.cpp handles vkDestroy calls.
    delete pipeline;
}

JNIEXPORT jint JNICALL
Java_fastgpu_FastGPUImpl_nativeDispatchKernel(JNIEnv* env, jclass, jlong handle, jlong kernelHandle,
                                              jint x, jint y, jint z,
                                              jlongArray jbuffers, jlongArray jimages) {
    auto* ctx = reinterpret_cast<vk::VulkanContext*>(handle);
    auto* pipeline = reinterpret_cast<vk::Pipeline*>(kernelHandle);
    
    std::vector<vk::Buffer*> vkBuffers;
    if (jbuffers != nullptr) {
        jsize len = env->GetArrayLength(jbuffers);
        jlong* bufferHandles = env->GetLongArrayElements(jbuffers, nullptr);
        for (int i = 0; i < len; i++) {
            vkBuffers.push_back(reinterpret_cast<vk::Buffer*>(bufferHandles[i]));
        }
        env->ReleaseLongArrayElements(jbuffers, bufferHandles, JNI_ABORT);
    }

    std::vector<vk::Image*> vkImages;
    if (jimages != nullptr) {
        jsize len = env->GetArrayLength(jimages);
        jlong* imageHandles = env->GetLongArrayElements(jimages, nullptr);
        for (int i = 0; i < len; i++) {
            vkImages.push_back(reinterpret_cast<vk::Image*>(imageHandles[i]));
        }
        env->ReleaseLongArrayElements(jimages, imageHandles, JNI_ABORT);
    }

    vk::dispatchCompute(*ctx, *pipeline, x, y, z, vkBuffers, vkImages);
    return 0;
}

}

