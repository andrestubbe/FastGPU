#pragma once

#include <vulkan/volk.h>
#include "vk_context.h"
#include <cstdint>

namespace vk {

uint32_t findMemoryType(VkPhysicalDevice physicalDevice, uint32_t typeFilter, VkMemoryPropertyFlags properties);

} // namespace vk
