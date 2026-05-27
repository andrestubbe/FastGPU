#pragma once

#include <vulkan/volk.h>
#include "vk_context.h"
#include <cstdint>

namespace vk {

struct Buffer {
    VkBuffer buffer = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkDeviceSize size = 0;
    void* mapped = nullptr;
};

Buffer createBuffer(const VulkanContext& ctx, VkDeviceSize size, VkBufferUsageFlags usage, VkMemoryPropertyFlags properties);
void destroyBuffer(const VulkanContext& ctx, Buffer& buffer);
void uploadToBuffer(const VulkanContext& ctx, Buffer& buffer, const void* data, size_t size);
void downloadFromBuffer(const VulkanContext& ctx, const Buffer& buffer, void* data, size_t size);

} // namespace vk
