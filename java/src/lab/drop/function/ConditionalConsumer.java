package lab.drop.function;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A conditional consumer, having an acceptance logic for either outputs of the predicate.
 * @param predicate The predicate to decide the acceptance logic.
 * @param positive The acceptance logic for positive inputs.
 * @param negative The acceptance logic for negative inputs.
 * @param <I> The input type.
 */
public record ConditionalConsumer<I>(Predicate<I> predicate, Consumer<I> positive, Consumer<I> negative)
        implements Consumer<I> {

    /**
     * Constructs a conditional consumer, having an acceptance logic for positive outputs of the predicate only.
     * @param predicate The predicate to decide the acceptance.
     * @param positive The acceptance logic for positive inputs.
     */
    public ConditionalConsumer(Predicate<I> predicate, Consumer<I> positive) {
        this(predicate, positive, t -> {});
    }

    @Override
    public void accept(I t) {
        (predicate.test(t) ? positive : negative).accept(t);
    }
}
