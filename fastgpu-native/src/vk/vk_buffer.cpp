#include "vk_buffer.h"
#include "vk_memory.h"
#include <stdexcept>
#include <cstring>

namespace vk {

Buffer createBuffer(const VulkanContext& ctx, VkDeviceSize size, VkBufferUsageFlags usage, VkMemoryPropertyFlags properties) {
    Buffer result;
    result.size = size;

    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    if (vkCreateBuffer(ctx.device, &bufferInfo, nullptr, &result.buffer) != VK_SUCCESS) {
        throw std::runtime_error("Failed to create buffer");
    }

    VkMemoryRequirements memRequirements;
    vkGetBufferMemoryRequirements(ctx.device, result.buffer, &memRequirements);

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(ctx.physicalDevice, memRequirements.memoryTypeBits, properties);

    if (vkAllocateMemory(ctx.device, &allocInfo, nullptr, &result.memory) != VK_SUCCESS) {
        throw std::runtime_error("Failed to allocate buffer memory");
    }

    vkBindBufferMemory(ctx.device, result.buffer, result.memory, 0);

    // Map if it's host visible
    if ((properties & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
        vkMapMemory(ctx.device, result.memory, 0, size, 0, &result.mapped);
    }

    return result;
}

void destroyBuffer(const VulkanContext& ctx, Buffer& buffer) {
    if (buffer.mapped) {
        vkUnmapMemory(ctx.device, buffer.memory);
        buffer.mapped = nullptr;
    }
    if (buffer.buffer != VK_NULL_HANDLE) {
        vkDestroyBuffer(ctx.device, buffer.buffer, nullptr);
        buffer.buffer = VK_NULL_HANDLE;
    }
    if (buffer.memory != VK_NULL_HANDLE) {
        vkFreeMemory(ctx.device, buffer.memory, nullptr);
        buffer.memory = VK_NULL_HANDLE;
    }
}

void uploadToBuffer(const VulkanContext& ctx, Buffer& buffer, const void* data, size_t size) {
    if (!buffer.mapped) {
        throw std::runtime_error("Cannot upload to unmapped buffer");
    }
    std::memcpy(buffer.mapped, data, size);
    
    // For non-coherent memory, we would need to vkFlushMappedMemoryRanges here.
    // We assume HOST_COHERENT for simplicity on UMA.
}

void downloadFromBuffer(const VulkanContext& ctx, const Buffer& buffer, void* data, size_t size) {
    if (!buffer.mapped) {
        throw std::runtime_error("Cannot download from unmapped buffer");
    }
    
    // For non-coherent memory, we would need to vkInvalidateMappedMemoryRanges here.
    std::memcpy(data, buffer.mapped, size);
}

} // namespace vk
