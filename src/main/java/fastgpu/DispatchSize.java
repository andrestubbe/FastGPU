package fastgpu;

public record DispatchSize(int x, int y, int z) {
    public static DispatchSize of1D(int x) {
        return new DispatchSize(x, 1, 1);
    }

    public static DispatchSize of2D(int x, int y) {
        return new DispatchSize(x, y, 1);
    }

    public static DispatchSize of3D(int x, int y, int z) {
        return new DispatchSize(x, y, z);
    }
}
