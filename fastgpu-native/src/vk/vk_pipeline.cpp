#include "vk_pipeline.h"
#include <stdexcept>
#include <iostream>

namespace vk {

Pipeline createPipeline(const VulkanContext& ctx, const std::string& source, const std::string& lang) {
    Pipeline result;
    
    // In v0.2, without a GLSL to SPIR-V compiler integrated (like shaderc),
    // we cannot compile GLSL strings directly.
    if (lang == "GLSL_COMPUTE") {
        std::cerr << "[FastGPU] WARNING: GLSL to SPIR-V compilation is not yet integrated. "
                  << "Kernel compilation will be bypassed.\n";
        return result; // Return empty pipeline
    }

    if (lang == "SPIR_V") {
        // Assume source contains raw SPIR-V bytes, casted to string for transport
        VkShaderModuleCreateInfo createInfo{};
        createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
        createInfo.codeSize = source.size();
        createInfo.pCode = reinterpret_cast<const uint32_t*>(source.data());

        if (vkCreateShaderModule(ctx.device, &createInfo, nullptr, &result.shaderModule) != VK_SUCCESS) {
            throw std::runtime_error("Failed to create shader module from SPIR-V");
        }
    }

    // Usually here we would parse the SPIR-V to extract bindings, 
    // but for v0.2 we just create a dummy layout or return empty if no shader.
    // Full implementation requires spirv-cross or similar.
    return result;
}

void destroyPipeline(const VulkanContext& ctx, Pipeline& pipeline) {
    if (pipeline.computePipeline != VK_NULL_HANDLE) {
        vkDestroyPipeline(ctx.device, pipeline.computePipeline, nullptr);
    }
    if (pipeline.pipelineLayout != VK_NULL_HANDLE) {
        vkDestroyPipelineLayout(ctx.device, pipeline.pipelineLayout, nullptr);
    }
    if (pipeline.descriptorSetLayout != VK_NULL_HANDLE) {
        vkDestroyDescriptorSetLayout(ctx.device, pipeline.descriptorSetLayout, nullptr);
    }
    if (pipeline.shaderModule != VK_NULL_HANDLE) {
        vkDestroyShaderModule(ctx.device, pipeline.shaderModule, nullptr);
    }
}

} // namespace vk
