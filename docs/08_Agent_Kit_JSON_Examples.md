# FastGPU: Agent-Kit JSON Beispiele

Das Dokument spezifiziert genau, wie das Agent-Kit strukturiert sein soll, um einen KI-Agenten zu trainieren. Ein Agent kommuniziert über diese JSON-Payloads mit FastGPU.

## 1. Top-Level Command Schema
Dieses Schema verwendet der Agent, um FastGPU zu steuern:
```json
{
  "action": "dispatch_kernel",
  "kernel": "vector_add",
  "backend": "AUTO",
  "language": "GLSL_COMPUTE",
  "source": "#version 450...",
  "size": [1024, 1, 1],
  "buffers": {
    "a": { "role": "input",  "dtype": "float32", "size": 1024 },
    "b": { "role": "input",  "dtype": "float32", "size": 1024 },
    "out": { "role": "output", "dtype": "float32", "size": 1024 }
  }
}
```

## 2. Micro-Demos (z.B. vector_add.json)
Demos dienen als Referenzmaterial (RAG) für den Agenten.
```json
{
  "description": "Add two float vectors on GPU",
  "action": "dispatch_kernel",
  "kernel": "vector_add",
  "backend": "AUTO",
  "language": "GLSL_COMPUTE",
  "source": "...",
  "size": [1024, 1, 1],
  "buffers": { ... }
}
```

## 3. Failure Cases (Fehlererkennung)
Beispiel für eine falsche Dispatch-Größe (das Dispatch-Grid ist kleiner als die Buffer-Size). Der Agent soll lernen, dass er `OUT_OF_RANGE_OR_UNCOVERED_ELEMENTS` Fehler vermeidet.
```json
{
  "description": "Dispatch size smaller than buffer length",
  "action": "dispatch_kernel",
  "kernel": "vector_add",
  "size": [512, 1, 1], // ZU KLEIN!
  "buffers": {
    "a": { "role": "input", "dtype": "float32", "size": 1024 }
  },
  "expected_error": "OUT_OF_RANGE_OR_UNCOVERED_ELEMENTS"
}
```
