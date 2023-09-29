package lab.drop.pipeline;

import lab.drop.data.Data;
import lab.drop.flow.Flow;
import lab.drop.functional.UnsafeConsumer;
import lab.drop.pipeline.monitoring.PipeMonitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A queue of drops moved between pipeline workers.
 * @param <D> The drops type.
 */
public abstract class Pipe<D> implements PipeMonitoring {
    private static final long POLLING_TIMEOUT = 100;

    private final int baseCapacity;
    private final String name;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final BlockingQueue<Drop<D>> inOrderQueue;
    private final Map<Long, Drop<D>> outOfOrderDrops;
    private final AtomicInteger inPush = new AtomicInteger();
    private long expectedIndex = 0;
    private long totalsSum = 0;
    private Throwable endOfInput;

    /**
     * Constructs a pipe.
     * @param baseCapacity The base capacity (<code>BC</code>) of the pipe. Used as the capacity for the in-order queue,
     *                     as well as the out-of-order drops cache. Together with the in-push drops, which depends on
     *                     the number of the pushing threads <code>N</code>, the total maximum theoretical capacity of
     *                     the pipe can reach <code>BC+N</code>.
     * @param name The name of the pipe.
     */
    protected Pipe(int baseCapacity, String name) {
        this.baseCapacity = Data.requirePositive(baseCapacity);
        this.name = name;
        inOrderQueue = new ArrayBlockingQueue<>(baseCapacity, true);
        outOfOrderDrops = new HashMap<>();
    }

    /**
     * Returns the name of the pipe.
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public final int getBaseCapacity() {
        return baseCapacity;
    }

    @Override
    public int getInOrderDrops() {
        return inOrderQueue.size();
    }

    @Override
    public int getOutOfOrderDrops() {
        return outOfOrderDrops.size();
    }

    @Override
    public int getInPushDrops() {
        return inPush.get();
    }

    @Override
    public long getDropsPushed() {
        return expectedIndex;
    }

    @Override
    public double getAverageLoad() {
        return expectedIndex == 0 ? 0 : Math.min((double) totalsSum / (expectedIndex * baseCapacity), 1);
    }

    void push(Drop<D> drop) throws Exception {
        inPush.incrementAndGet();
        try {
            pushDrop(drop);
        } finally {
            inPush.decrementAndGet();
        }
    }

    private void pushDrop(Drop<D> drop) throws Exception {
        if (drop == null)
            return;
        Flow.throwIfNonNull(endOfInput);
        while (true) {
            lock.lockInterruptibly();
            try {
                if (tryPush(drop))
                    break;
            } finally {
                lock.unlock();
            }
            synchronized (lock) {
                lock.wait(POLLING_TIMEOUT);
            }
        }
    }

    private boolean tryPush(Drop<D> drop) throws Exception {
        if (drop.index() == expectedIndex) {
            inOrderQueue.put(drop);
            expectedIndex++;
            totalsSum += getTotalDrops() - 1;
        } else {
            if (outOfOrderDrops.size() > getInPushDrops())
                return false;
            outOfOrderDrops.put(drop.index(), drop);
        }
        pushDrop(outOfOrderDrops.remove(expectedIndex));
        synchronized (lock) {
            lock.notifyAll();
        }
        return true;
    }

    private Drop<D> take() throws InterruptedException {
        while (endOfInput == null) {
            Drop<D> drop = inOrderQueue.poll(POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
            if (drop != null)
                return drop;
        }
        return inOrderQueue.poll();
    }

    /**
     * Sets the pipe end-of-input "flag", indicating there's no more drops to poll from it. Pushing drops after setting
     * the flag will result in an exception.
     */
    void setEndOfInput() {
        setEndOfInput(null);
    }

    /**
     * Sets the pipe end-of-input "flag" as a throwable for cancellation flow, in case a push attempt will occur before
     * the canceled worker closes.
     */
    void setEndOfInput(Throwable throwable) {
        endOfInput = Objects.requireNonNullElse(throwable, new IllegalStateException(
                "Attempting to push into pipe after end of input."));
    }

    void drain() throws Exception {
        drain(i -> {});
    }

    void drain(UnsafeConsumer<Drop<D>> action) throws Exception {
        Flow.acceptWhile(this::take, action, Objects::nonNull);
    }

    void clear() throws InterruptedException {
        for (int i = 0; getTotalDrops() > 0; i++) {
            final int waitTime = i * 10 + 1;
            synchronized (lock) {
                lock.wait(waitTime);
            }
            inOrderQueue.clear();
            outOfOrderDrops.clear();
        }
    }

    @Override
    public String toString() {
        return String.format("-<%s:%d>-", getName(), baseCapacity);
    }
}
