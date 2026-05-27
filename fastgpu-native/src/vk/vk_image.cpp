#include "vk_image.h"
#include "vk_memory.h"
#include <stdexcept>
#include <cstring>

namespace vk {

Image createImage(const VulkanContext& ctx, uint32_t width, uint32_t height, VkFormat format) {
    Image result;
    result.width = width;
    result.height = height;
    result.format = format;

    // For UMA, we can use LINEAR tiling to map memory directly.
    // Optimal tiling is faster for GPU access, but requires a staging buffer.
    // For simplicity in v0.2, we'll try LINEAR if supported, else we'd need optimal + staging.
    // Since this is for Intel Iris, LINEAR compute is usually "okay", but optimal is better.
    // We'll use LINEAR to keep `upload`/`download` simple.
    
    VkImageCreateInfo imageInfo{};
    imageInfo.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    imageInfo.imageType = VK_IMAGE_TYPE_2D;
    imageInfo.extent.width = width;
    imageInfo.extent.height = height;
    imageInfo.extent.depth = 1;
    imageInfo.mipLevels = 1;
    imageInfo.arrayLayers = 1;
    imageInfo.format = format;
    imageInfo.tiling = VK_IMAGE_TILING_LINEAR;
    imageInfo.initialLayout = VK_IMAGE_LAYOUT_PREINITIALIZED;
    imageInfo.usage = VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
    imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;

    if (vkCreateImage(ctx.device, &imageInfo, nullptr, &result.image) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create image");
    }

    VkMemoryRequirements memRequirements;
    vkGetImageMemoryRequirements(ctx.device, result.image, &memRequirements);
    result.size = memRequirements.size;

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(ctx.physicalDevice, memRequirements.memoryTypeBits, 
                                                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

    if (vkAllocateMemory(ctx.device, &allocInfo, nullptr, &result.memory) != VK_SUCCESS) {
        throw std::runtime_error("Failed to allocate image memory");
    }

    vkBindImageMemory(ctx.device, result.image, result.memory, 0);

    vkMapMemory(ctx.device, result.memory, 0, result.size, 0, &result.mapped);

    // Create View
    VkImageViewCreateInfo viewInfo{};
    viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    viewInfo.image = result.image;
    viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
    viewInfo.format = format;
    viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    viewInfo.subresourceRange.baseMipLevel = 0;
    viewInfo.subresourceRange.levelCount = 1;
    viewInfo.subresourceRange.baseArrayLayer = 0;
    viewInfo.subresourceRange.layerCount = 1;

    if (vkCreateImageView(ctx.device, &viewInfo, nullptr, &result.view) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create image view");
    }

    return result;
}

void destroyImage(const VulkanContext& ctx, Image& image) {
    if (image.mapped) {
        vkUnmapMemory(ctx.device, image.memory);
        image.mapped = nullptr;
    }
    if (image.view != VK_NULL_HANDLE) {
        vkDestroyImageView(ctx.device, image.view, nullptr);
        image.view = VK_NULL_HANDLE;
    }
    if (image.image != VK_NULL_HANDLE) {
        vkDestroyImage(ctx.device, image.image, nullptr);
        image.image = VK_NULL_HANDLE;
    }
    if (image.memory != VK_NULL_HANDLE) {
        vkFreeMemory(ctx.device, image.memory, nullptr);
        image.memory = VK_NULL_HANDLE;
    }
}

void uploadToImage(const VulkanContext& ctx, Image& image, const void* data, size_t size) {
    if (!image.mapped) {
        throw std::runtime_error("Cannot upload to unmapped image");
    }
    
    // For linear tiling, row pitch might be different than width * 4.
    VkImageSubresource subresource{};
    subresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    VkSubresourceLayout layout;
    vkGetImageSubresourceLayout(ctx.device, image.image, &subresource, &layout);

    uint32_t bpp = 4; // Assuming RGBA8
    if (image.format == VK_FORMAT_R32_SFLOAT) bpp = 4;
    else if (image.format == VK_FORMAT_R32G32B32A32_SFLOAT) bpp = 16;

    if (layout.rowPitch == static_cast<VkDeviceSize>(image.width * bpp)) {
        std::memcpy(image.mapped, data, size);
    } else {
        const uint8_t* src = static_cast<const uint8_t*>(data);
        uint8_t* dst = static_cast<uint8_t*>(image.mapped);
        for (uint32_t y = 0; y < image.height; y++) {
            std::memcpy(dst + y * layout.rowPitch, src + y * (image.width * bpp), image.width * bpp);
        }
    }
}

void downloadFromImage(const VulkanContext& ctx, const Image& image, void* data, size_t size) {
    if (!image.mapped) {
        throw std::runtime_error("Cannot download from unmapped image");
    }
    
    VkImageSubresource subresource{};
    subresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    VkSubresourceLayout layout;
    vkGetImageSubresourceLayout(ctx.device, image.image, &subresource, &layout);

    uint32_t bpp = 4; // Assuming RGBA8
    if (image.format == VK_FORMAT_R32_SFLOAT) bpp = 4;
    else if (image.format == VK_FORMAT_R32G32B32A32_SFLOAT) bpp = 16;

    if (layout.rowPitch == static_cast<VkDeviceSize>(image.width * bpp)) {
        std::memcpy(data, image.mapped, size);
    } else {
        uint8_t* dst = static_cast<uint8_t*>(data);
        const uint8_t* src = static_cast<const uint8_t*>(image.mapped);
        for (uint32_t y = 0; y < image.height; y++) {
            std::memcpy(dst + y * (image.width * bpp), src + y * layout.rowPitch, image.width * bpp);
        }
    }
}

} // namespace vk
