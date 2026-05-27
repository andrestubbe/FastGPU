# FastGPU: IR & Compiler (Experimental)

Zusätzlich zum Ausführen von GLSL-Compute-Shadern strebt FastGPU an, native Java-Lambdas direkt auf der GPU auszuführen.

## Der Flow: Lambda -> IR -> SPIR-V

1. **Java Lambda:** Der Entwickler schreibt Standard-Java-Code.
2. **Bytecode Parsing:** Der Bytecode des Lambdas wird analysiert.
3. **FastIR:** Umwandlung in eine minimalistische, eigene Intermediate Representation (IR).
4. **SPIR-V Generation:** Übersetzung der IR in binäres SPIR-V für das Vulkan-Backend.

## FastIR Nodes (Minimalistisches Design)
```java
sealed interface FastIRNode permits FastIRBinaryOp, FastIRLoad, FastIRStore, FastIRIndex, FastIRConst {}

record FastIRBinaryOp(String op, FastIRNode a, FastIRNode b) implements FastIRNode {}
record FastIRLoad(String buffer, FastIRNode index) implements FastIRNode {}
record FastIRStore(String buffer, FastIRNode index, FastIRNode value) implements FastIRNode {}
record FastIRIndex(String varName) implements FastIRNode {}
record FastIRConst(float value) implements FastIRNode {}
```

## Beispiel-Nutzung
```java
FastGPUKernel k = gpu.compileLambda((i, ctx) -> {
    float v = ctx.load(a, i) + ctx.load(b, i);
    ctx.store(out, i, v);
});
```

Dieser Ansatz bietet Babylon-ähnlichen Komfort, bleibt aber extrem leichtgewichtig und fokussiert sich ausschließlich auf Compute-Workloads (SIMD/SPMD).
