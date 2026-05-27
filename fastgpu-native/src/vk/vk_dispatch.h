#pragma once

#include <vulkan/volk.h>
#include "vk_context.h"
#include "vk_pipeline.h"
#include <vector>

namespace vk {

void dispatchCompute(const VulkanContext& ctx, const Pipeline& pipeline, uint32_t x, uint32_t y, uint32_t z, const std::vector<VkBuffer>& buffers);

} // namespace vk
