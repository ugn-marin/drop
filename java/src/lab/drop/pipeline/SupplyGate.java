package lab.drop.pipeline;

import lab.drop.function.UnsafeConsumer;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An entry point of drops in a new index scope.
 * @param <S> The supplied drops type.
 */
@FunctionalInterface
public interface SupplyGate<S> {

    /**
     * Pushes a drop using this supply gate's index scope.
     * @param drop The drop.
     * @throws InterruptedException If interrupted while attempting to push the drop.
     */
    void push(S drop) throws InterruptedException;

    /**
     * Pushes all the drops from the stream into the supply gate.
     * @param stream The stream.
     * @throws InterruptedException If interrupted while attempting to push a drop.
     */
    default void pushAll(Stream<S> stream) throws InterruptedException {
        var iterator = Objects.requireNonNull(stream, "Stream is null.").sequential().iterator();
        while (iterator.hasNext())
            push(iterator.next());
    }

    /**
     * Wraps this supply gate implementation in an interruptible Consumer.
     */
    default UnsafeConsumer<S> toConsumer() {
        return this::push;
    }
}
