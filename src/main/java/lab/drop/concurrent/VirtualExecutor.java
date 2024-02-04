package lab.drop.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

record VirtualExecutor(Lazy<ExecutorService> executor) implements ConcurrentExecutor {

    VirtualExecutor() {
        this(new Lazy<>(Executors::newVirtualThreadPerTaskExecutor));
    }
}
