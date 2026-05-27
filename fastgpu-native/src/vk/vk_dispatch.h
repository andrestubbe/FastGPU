#pragma once

#include <vulkan/volk.h>
#include "vk_context.h"
#include "vk_pipeline.h"
#include "vk_buffer.h"
#include "vk_image.h"
#include <vector>

namespace vk {

void dispatchCompute(const VulkanContext& ctx, const Pipeline& pipeline, uint32_t x, uint32_t y, uint32_t z, const std::vector<Buffer*>& buffers, const std::vector<Image*>& images);

} // namespace vk
