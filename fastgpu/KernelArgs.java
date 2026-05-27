package fastgpu;

import java.util.Arrays;
import java.util.List;

public final class KernelArgs {

    private final List<Object> args;

    private KernelArgs(List<Object> args) {
        this.args = List.copyOf(args);
    }

    public static KernelArgs of(Object... args) {
        return new KernelArgs(Arrays.asList(args));
    }

    List<Object> args() {
        return args;
    }
}
