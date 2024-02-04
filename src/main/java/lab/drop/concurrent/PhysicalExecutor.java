package lab.drop.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

record PhysicalExecutor(Lazy<ExecutorService> executor) implements ConcurrentExecutor {

    PhysicalExecutor() {
        this(new Lazy<>(() -> Executors.newCachedThreadPool(Concurrent.namedThreadFactory("Physical", true))));
    }
}
