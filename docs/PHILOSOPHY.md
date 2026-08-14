# FastGPU Engineering Philosophy

## Core Architecture Principles

1. **Zero-Copy Memory Transfers**  
   FastGPU bypasses standard JVM heap serialization by sharing direct off-heap byte buffers between Java (`sun.misc.Unsafe` / `DirectByteBuffer`) and native GPU VRAM memory pointers.

2. **Vulkan 1.3 & Apple Metal Compute First**  
   Prioritizes cross-platform compute shader pipelines (SPIR-V on Windows/Linux, MSL on macOS) for tensor matrix operations in local AI engines (**FastAIModel**, **FastAI**).

3. **Minimalist JNI Bridge**  
   Eliminates Java object allocation inside JNI calls to achieve sub-0.04 ms compute dispatch latencies and 1.4B+ ops/sec JMH throughput.
