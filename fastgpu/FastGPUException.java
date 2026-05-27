package fastgpu;

public class FastGPUException extends RuntimeException {
    public FastGPUException(String message) {
        super(message);
    }

    public FastGPUException(String message, Throwable cause) {
        super(message, cause);
    }
}
