#pragma once

#include <vulkan/volk.h>
#include "vk_context.h"
#include <cstdint>

namespace vk {

struct Image {
    VkImage image = VK_NULL_HANDLE;
    VkImageView view = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkFormat format;
    uint32_t width;
    uint32_t height;
    void* mapped = nullptr;
    VkDeviceSize size = 0;
};

Image createImage(const VulkanContext& ctx, uint32_t width, uint32_t height, VkFormat format);
void destroyImage(const VulkanContext& ctx, Image& image);
void uploadToImage(const VulkanContext& ctx, Image& image, const void* data, size_t size);
void downloadFromImage(const VulkanContext& ctx, const Image& image, void* data, size_t size);

} // namespace vk
