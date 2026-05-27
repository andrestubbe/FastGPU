#include "vk_dispatch.h"
#include <stdexcept>
#include <iostream>

namespace vk {

void dispatchCompute(const VulkanContext& ctx, const Pipeline& pipeline, uint32_t x, uint32_t y, uint32_t z, const std::vector<Buffer*>& buffers, const std::vector<Image*>& images) {
    if (pipeline.computePipeline == VK_NULL_HANDLE) {
        std::cerr << "[FastGPU] Bypassing dispatch (Pipeline not initialized)\n";
        return;
    }

    // Allocate Descriptor Set
    VkDescriptorSetAllocateInfo allocSetInfo{};
    allocSetInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    allocSetInfo.descriptorPool = ctx.descriptorPool;
    allocSetInfo.descriptorSetCount = 1;
    allocSetInfo.pSetLayouts = &pipeline.descriptorSetLayout;

    VkDescriptorSet descriptorSet;
    if (vkAllocateDescriptorSets(ctx.device, &allocSetInfo, &descriptorSet) != VK_SUCCESS) {
        throw std::runtime_error("Failed to allocate descriptor set");
    }

    // Update Descriptor Set
    std::vector<VkWriteDescriptorSet> descriptorWrites;
    std::vector<VkDescriptorBufferInfo> bufferInfos(buffers.size());
    std::vector<VkDescriptorImageInfo> imageInfos(images.size());

    for (size_t i = 0; i < buffers.size(); i++) {
        if (!buffers[i]) continue;
        bufferInfos[i].buffer = buffers[i]->buffer;
        bufferInfos[i].offset = 0;
        bufferInfos[i].range = VK_WHOLE_SIZE;

        VkWriteDescriptorSet write{};
        write.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
        write.dstSet = descriptorSet;
        write.dstBinding = static_cast<uint32_t>(i); // Buffers mapped to binding 0-3
        write.dstArrayElement = 0;
        write.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
        write.descriptorCount = 1;
        write.pBufferInfo = &bufferInfos[i];
        descriptorWrites.push_back(write);
    }

    for (size_t i = 0; i < images.size(); i++) {
        if (!images[i]) continue;
        imageInfos[i].imageLayout = VK_IMAGE_LAYOUT_GENERAL;
        imageInfos[i].imageView = images[i]->view;
        imageInfos[i].sampler = VK_NULL_HANDLE;

        VkWriteDescriptorSet write{};
        write.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
        write.dstSet = descriptorSet;
        write.dstBinding = static_cast<uint32_t>(i + 4); // Images mapped to binding 4-7
        write.dstArrayElement = 0;
        write.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
        write.descriptorCount = 1;
        write.pImageInfo = &imageInfos[i];
        descriptorWrites.push_back(write);
    }

    if (!descriptorWrites.empty()) {
        vkUpdateDescriptorSets(ctx.device, static_cast<uint32_t>(descriptorWrites.size()), descriptorWrites.data(), 0, nullptr);
    }

    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.commandPool = ctx.commandPool;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = 1;

    VkCommandBuffer commandBuffer;
    if (vkAllocateCommandBuffers(ctx.device, &allocInfo, &commandBuffer) != VK_SUCCESS) {
        throw std::runtime_error("Failed to allocate command buffer");
    }

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

    vkBeginCommandBuffer(commandBuffer, &beginInfo);
    
    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.computePipeline);
    
    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.pipelineLayout, 0, 1, &descriptorSet, 0, nullptr);

    vkCmdDispatch(commandBuffer, x, y, z);

    vkEndCommandBuffer(commandBuffer);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;

    if (vkQueueSubmit(ctx.computeQueue, 1, &submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
        throw std::runtime_error("Failed to submit compute command buffer");
    }

    VkResult waitResult = vkQueueWaitIdle(ctx.computeQueue);
    vkFreeCommandBuffers(ctx.device, ctx.commandPool, 1, &commandBuffer);
    vkFreeDescriptorSets(ctx.device, ctx.descriptorPool, 1, &descriptorSet);
    
    if (waitResult != VK_SUCCESS) {
        throw std::runtime_error("vkQueueWaitIdle failed (possible TDR/Device Lost)");
    }
}

} // namespace vk
