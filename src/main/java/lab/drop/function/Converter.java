package lab.drop.function;

import lab.drop.Sugar;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A function converting inputs according to their types matching the provided functions map. Inputs that don't match
 * any of the mapped types can be handled by an optional Else function (by default converter to null). Null inputs are
 * always returned as null.
 * @param <I> The input type.
 * @param <O> The output type.
 */
public class Converter<I, O> implements Function<I, O> {
    private final Map<Class<? extends I>, Function<I, O>> matches;
    private Function<I, O> orElse;
    private final Reducer<Class<? extends I>> reducer;

    /**
     * Constructs an empty converter.
     */
    public Converter() {
        this(Map.of());
    }

    /**
     * Constructs a converter.
     * @param matches The map of functions converting inputs according to their type. If none match, converts to null.
     */
    public Converter(Map<Class<? extends I>, Function<? extends I, O>> matches) {
        this(matches, t -> null);
    }

    /**
     * Constructs a converter.
     * @param matches The map of functions converting inputs according to their type.
     * @param orElse The converting function for inputs that don't match any of the mapped types.
     */
    public Converter(Map<Class<? extends I>, Function<? extends I, O>> matches, Function<? extends I, O> orElse) {
        this.matches = new HashMap<>(Objects.requireNonNull(matches, "Matches are null.").size());
        matches.forEach((key, value) -> put(Sugar.cast(key), value));
        orElse(orElse);
        reducer = Reducer.from((c1, c2) -> c1.isAssignableFrom(c2) ? c2 : c1);
    }

    /**
     * Puts a specific subtype converting function mapping in this converter.
     * @param type The input type.
     * @param function The input converting function.
     * @param <S> The subtype.
     * @return This converter.
     */
    public <S extends I> Converter<I, O> put(Class<S> type, Function<S, O> function) {
        matches.put(Objects.requireNonNull(type, "Type is null."),
                Sugar.cast(Objects.requireNonNull(function, "Function is null.")));
        return this;
    }

    /**
     * Sets a new converting function for inputs that don't match any of the mapped types.
     * @param function The input converting function.
     * @return This converter.
     */
    public Converter<I, O> orElse(Function<? extends I, O> function) {
        this.orElse = Sugar.cast(Objects.requireNonNull(function, "Function is null."));
        return this;
    }

    /**
     * Sets the function for inputs that don't match any of the mapped types to throw an IllegalArgumentException.
     * @return This converter.
     */
    public Converter<I, O> orElseThrow() {
        return orElse(o -> { throw new IllegalArgumentException("Unmapped input type: " + o + "."); });
    }

    @Override
    public O apply(I t) {
        if (t == null)
            return null;
        var types = matches.keySet().stream().filter(type -> type.isAssignableFrom(t.getClass())).toList();
        return types.isEmpty() ? orElse.apply(t) : matches.get(reducer.apply(types)).apply(t);
    }
}
