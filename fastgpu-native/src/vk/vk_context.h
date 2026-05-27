#pragma once

#include <vulkan/volk.h>
#include <stdexcept>
#include <string>
#include <vector>

namespace vk {

struct VulkanContext {
    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkDevice device = VK_NULL_HANDLE;
    VkQueue computeQueue = VK_NULL_HANDLE;
    uint32_t queueFamilyIndex = 0;
    VkCommandPool commandPool = VK_NULL_HANDLE;

    VulkanContext();
    ~VulkanContext();

    // Disable copy/move
    VulkanContext(const VulkanContext&) = delete;
    VulkanContext& operator=(const VulkanContext&) = delete;

private:
    void createInstance();
    void pickPhysicalDevice();
    void createDevice();
    void createCommandPool();
};

} // namespace vk
