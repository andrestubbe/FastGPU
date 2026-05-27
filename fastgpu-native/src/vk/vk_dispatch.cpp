#include "vk_dispatch.h"
#include <stdexcept>
#include <iostream>

namespace vk {

void dispatchCompute(const VulkanContext& ctx, const Pipeline& pipeline, uint32_t x, uint32_t y, uint32_t z, const std::vector<VkBuffer>& buffers) {
    if (pipeline.computePipeline == VK_NULL_HANDLE) {
        // Fallback for v0.2 stub if shader wasn't compiled
        std::cerr << "[FastGPU] Bypassing dispatch (Pipeline not initialized)\n";
        return;
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
    
    // In a full implementation, we'd bind descriptor sets here:
    // vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.pipelineLayout, 0, 1, &descriptorSet, 0, nullptr);

    vkCmdDispatch(commandBuffer, x, y, z);

    vkEndCommandBuffer(commandBuffer);

    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;

    if (vkQueueSubmit(ctx.computeQueue, 1, &submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
        throw std::runtime_error("Failed to submit compute command buffer");
    }

    vkQueueWaitIdle(ctx.computeQueue);
    vkFreeCommandBuffers(ctx.device, ctx.commandPool, 1, &commandBuffer);
}

} // namespace vk
