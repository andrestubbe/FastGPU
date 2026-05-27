# FastGPU: Agent-Kit

**WICHTIG:** Ein KI-Agent darf **niemals** Low-Level Vulkan/C++ Code generieren. Agenten sind zustandslos und fehleranfällig bei APIs, die streng deterministisches Memory-Management erfordern. Ein Agent interagiert *ausschließlich* über die High-Level Java-API oder über standardisierte JSON-Commands.

## Was der Agent sehen darf:
1. **Die Java High-Level API** (`allocFloat`, `upload`, `compile`, `dispatch`, etc.)
2. **JSON-Command-Schema** für agentengesteuerte Workloads.
3. **Demos** als Referenz (VectorAdd, GEMM, Blur).
4. **Benchmarks**, um entscheiden zu können, wann GPU-Einsatz sinnvoll ist.
5. **Failure-Cases**, um häufige Fehler (z.B. falsche Dispatch-Größen) zu vermeiden.

## JSON Command Schema
Wenn der Agent FastGPU über eine Text-Schnittstelle (z.B. MCP) steuert, verwendet er dieses Schema:
```json
{
  "action": "dispatch_kernel",
  "kernel": "string_name",
  "backend": "AUTO | VULKAN | CUDA",
  "language": "GLSL_COMPUTE | PTX | SPIR_V",
  "source": "string",
  "size": [x, y, z],
  "buffers": {
    "buffer_name": { "role": "input|output", "dtype": "float32", "size": 1024 }
  }
}
```

## Failure Cases (Zur Agenten-Schulung)
- **wrong_dispatch_size:** Das Dispatch-Grid deckt nicht alle Elemente des Buffers ab, was zu unvollständigen Berechnungen führt.
- **missing_synchronization:** Download aufgerufen, bevor der Kernel-Dispatch via Fence beendet ist (wird von der High-Level API automatisch verhindert).
- **out_of_bounds_access:** Kernel greift auf Index `i > buffer.size` zu. GPU-Crash möglich. Agent muss Bounds-Checking in den Shader einbauen: `if (i >= max_size) return;`
