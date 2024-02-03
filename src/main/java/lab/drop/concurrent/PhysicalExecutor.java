package lab.drop.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PhysicalExecutor implements ConcurrentExecutor {
    private final Lazy<ExecutorService> cachedPool = new Lazy<>(() -> Executors.newCachedThreadPool(
            Concurrent.namedThreadFactory(Concurrent.class.getSimpleName(), true)));

    @Override
    public Lazy<ExecutorService> executor() {
        return cachedPool;
    }
}
