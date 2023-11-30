package lab.drop.flow;

import lab.drop.concurrent.LazyAutoCloseable;
import lab.drop.data.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

class AutoCloseablePoolTest {

    private static class Resource implements AutoCloseable {
        int usage = 0;

        void use() {
            usage++;
        }

        @Override
        public void close() {
            usage = -1;
        }
    }

    @Test
    void initialSize() throws Exception {
        var additionalResources = Data.fill(10, Resource::new);
        AutoCloseablePool<Resource> pool;
        try (var lazyPool = new LazyAutoCloseable<>(() -> new AutoCloseablePool<>(Resource::new, 5, 10, 0))) {
            pool = lazyPool.get();
            Assertions.assertEquals(5, pool.size());
            AtomicReference<Resource> used = new AtomicReference<>();
            pool.use(res -> {
                Assertions.assertEquals(4, pool.size());
                res.use();
                Assertions.assertEquals(1, res.usage);
                used.set(res);
            });
            Assertions.assertEquals(1, used.get().usage);
            Assertions.assertEquals(5, pool.size());
            additionalResources.forEach(pool);
            Assertions.assertEquals(10, pool.size());
        }
        Assertions.assertEquals(0, pool.size());
        additionalResources.forEach(res -> Assertions.assertEquals(-1, res.usage));
    }
}
