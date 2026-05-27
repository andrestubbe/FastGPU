#pragma once

#include <vulkan/volk.h>
#include "vk_context.h"
#include <string>

namespace vk {

struct Pipeline {
    VkShaderModule shaderModule = VK_NULL_HANDLE;
    VkDescriptorSetLayout descriptorSetLayout = VK_NULL_HANDLE;
    VkPipelineLayout pipelineLayout = VK_NULL_HANDLE;
    VkPipeline computePipeline = VK_NULL_HANDLE;
};

Pipeline createPipeline(const VulkanContext& ctx, const std::string& source, const std::string& lang);
void destroyPipeline(const VulkanContext& ctx, Pipeline& pipeline);

} // namespace vk
