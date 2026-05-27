#include "vk_pipeline.h"
#include <stdexcept>
#include <iostream>
#include <fstream>
#include <cstdlib>
#include <vector>

namespace vk {

Pipeline createPipeline(const VulkanContext& ctx, const std::string& source, const std::string& lang) {
    Pipeline result;
    
    std::vector<char> spirvCode;

    if (lang == "GLSL_COMPUTE") {
        // Write GLSL to a temporary file
        std::ofstream out("fastgpu_temp.comp");
        if (!out) {
            throw std::runtime_error("Failed to write temporary shader file");
        }
        out << source;
        out.close();

        // Call glslc to compile
        std::cout << "[FastGPU] Compiling GLSL via glslc...\n";
        int ret = std::system("glslc fastgpu_temp.comp -o fastgpu_temp.spv");
        if (ret != 0) {
            std::cerr << "[FastGPU] ERROR: glslc failed! Make sure Vulkan SDK is installed and glslc is in PATH.\n";
            return result; // return empty pipeline
        }

        // Read the compiled SPIR-V
        std::ifstream in("fastgpu_temp.spv", std::ios::ate | std::ios::binary);
        if (!in) {
            std::cerr << "[FastGPU] ERROR: Failed to read compiled SPIR-V.\n";
            return result;
        }
        size_t fileSize = (size_t)in.tellg();
        spirvCode.resize(fileSize);
        in.seekg(0);
        in.read(spirvCode.data(), fileSize);
        in.close();

        // Cleanup temp files
        std::remove("fastgpu_temp.comp");
        std::remove("fastgpu_temp.spv");

    } else if (lang == "SPIR_V") {
        spirvCode.assign(source.begin(), source.end());
    }

    if (!spirvCode.empty()) {
        VkShaderModuleCreateInfo createInfo{};
        createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
        createInfo.codeSize = spirvCode.size();
        createInfo.pCode = reinterpret_cast<const uint32_t*>(spirvCode.data());

        if (vkCreateShaderModule(ctx.device, &createInfo, nullptr, &result.shaderModule) != VK_SUCCESS) {
            throw std::runtime_error("Failed to create shader module");
        }

        // 1. Create Descriptor Set Layout
        // For a compute shader, we typically have a single descriptor set with bindings for buffers/images.
        // Since we don't have a SPIR-V reflector like spirv-cross in v0.2, 
        // we'll create a generic layout with 4 storage buffers for VectorAddDemo
        // OR 1 storage image for ImageBlurDemo.
        // To keep it simple, we'll bind 4 storage buffers AND 4 storage images.
        // It's a bit hacky, but avoids needing a full SPIR-V reflection library.
        
        std::vector<VkDescriptorSetLayoutBinding> bindings;
        for (uint32_t i = 0; i < 4; i++) {
            VkDescriptorSetLayoutBinding bufferBinding{};
            bufferBinding.binding = i;
            bufferBinding.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
            bufferBinding.descriptorCount = 1;
            bufferBinding.stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
            bindings.push_back(bufferBinding);

            VkDescriptorSetLayoutBinding imageBinding{};
            imageBinding.binding = i + 4; // images at binding 4,5,6,7
            imageBinding.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
            imageBinding.descriptorCount = 1;
            imageBinding.stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
            bindings.push_back(imageBinding);
        }

        VkDescriptorSetLayoutCreateInfo layoutInfo{};
        layoutInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
        layoutInfo.bindingCount = static_cast<uint32_t>(bindings.size());
        layoutInfo.pBindings = bindings.data();

        if (vkCreateDescriptorSetLayout(ctx.device, &layoutInfo, nullptr, &result.descriptorSetLayout) != VK_SUCCESS) {
            throw std::runtime_error("Failed to create descriptor set layout");
        }

        // 2. Create Pipeline Layout
        VkPipelineLayoutCreateInfo pipelineLayoutInfo{};
        pipelineLayoutInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
        pipelineLayoutInfo.setLayoutCount = 1;
        pipelineLayoutInfo.pSetLayouts = &result.descriptorSetLayout;

        if (vkCreatePipelineLayout(ctx.device, &pipelineLayoutInfo, nullptr, &result.pipelineLayout) != VK_SUCCESS) {
            throw std::runtime_error("Failed to create pipeline layout");
        }

        // 3. Create Compute Pipeline
        VkComputePipelineCreateInfo pipelineInfo{};
        pipelineInfo.sType = VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
        pipelineInfo.stage.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
        pipelineInfo.stage.stage = VK_SHADER_STAGE_COMPUTE_BIT;
        pipelineInfo.stage.module = result.shaderModule;
        pipelineInfo.stage.pName = "main";
        pipelineInfo.layout = result.pipelineLayout;

        if (vkCreateComputePipelines(ctx.device, VK_NULL_HANDLE, 1, &pipelineInfo, nullptr, &result.computePipeline) != VK_SUCCESS) {
            throw std::runtime_error("Failed to create compute pipeline");
        }
        
        std::cout << "[FastGPU] Compute Pipeline successfully created!\n";
    }

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
