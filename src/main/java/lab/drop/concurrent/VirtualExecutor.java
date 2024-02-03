package lab.drop.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class VirtualExecutor implements ConcurrentExecutor {
    private final Lazy<ExecutorService> virtualPool = new Lazy<>(Executors::newVirtualThreadPerTaskExecutor);

    @Override
    public Lazy<ExecutorService> executor() {
        return virtualPool;
    }
}
