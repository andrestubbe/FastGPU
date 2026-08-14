package fastgpu.benchmark;

import fastgpu.FastGPU;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class FastGpuJmhBenchmark {

    private FastGPU gpu;

    @Setup
    public void setup() {
        gpu = new FastGPU();
    }

    @Benchmark
    public String testGetDeviceName() {
        return gpu.getDeviceName();
    }

    @TearDown
    public void tearDown() {
        if (gpu != null) {
            gpu.close();
        }
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
