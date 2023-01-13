package lab.drop.function;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A conditional function, having a computing logic for either outputs of the predicate.
 * @param predicate The predicate to decide the computing logic.
 * @param positive The computing logic for positive inputs.
 * @param negative The computing logic for negative inputs.
 * @param <I> The input type.
 * @param <O> The output type.
 */
public record ConditionalFunction<I, O>(Predicate<I> predicate, Function<I, O> positive, Function<I, O> negative)
        implements Function<I, O> {

    @Override
    public O apply(I t) {
        return (predicate.test(t) ? positive : negative).apply(t);
    }
}
