package lab.drop.concurrent;

import lab.drop.Sugar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class BlockingThreadPoolExecutorTest {

    @Test
    void order() throws InterruptedException {
        var pool = new BlockingThreadPoolExecutor();
        int tasks = 100;
        Set<Integer> finished = new HashSet<>();
        Sugar.iterate(tasks, task -> pool.execute(() -> {
            Interruptible.sleep(20);
            Assertions.assertTrue(finished.stream().noneMatch(n -> n > task));
            finished.add(task);
        }));
        Assertions.assertTrue(finished.size() < tasks);
        Concurrent.join(pool);
        Assertions.assertEquals(tasks, finished.size());
    }
}
