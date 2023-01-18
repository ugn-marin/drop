package lab.drop.concurrent;

import lab.drop.Sugar;
import lab.drop.calc.Units;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class BlockingThreadPoolExecutorTest {

    @Test
    void order() throws InterruptedException {
        var pool = new BlockingThreadPoolExecutor();
        int tasks = 100;
        int each = 5;
        Set<Integer> finished = new HashSet<>();
        long start = System.currentTimeMillis();
        Sugar.iterate(tasks, task -> pool.execute(() -> {
            Interruptible.sleep(each);
            Assertions.assertTrue(finished.stream().noneMatch(n -> n > task));
            Assertions.assertTrue(pool.getBlocked() <= 1);
            finished.add(task);
        }));
        Assertions.assertTrue(finished.size() < tasks);
        Assertions.assertTrue(Units.Time.since(start) >= (tasks - 1) * each);
        Assertions.assertEquals(0, pool.getBlocked());
        Concurrent.join(pool);
        Assertions.assertEquals(tasks, finished.size());
    }
}
