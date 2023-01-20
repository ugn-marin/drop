package lab.drop.function;

import lab.drop.Sugar;

import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A function reducing a list of items to a single item.
 * @param <T> The items type.
 */
public interface Reducer<T> extends Function<List<T>, T> {

    /**
     * Returns a decorator applying reducer, then the operator on the result.
     * @param after The operator to apply to the reducer result.
     * @return The reducer decorator.
     */
    default Reducer<T> andThen(UnaryOperator<T> after) {
        return items -> after.apply(apply(items));
    }

    /**
     * Returns a decorator applying reducer to non-empty lists, or returning the supplier result on null or empty lists.
     * @param onNullOrEmpty The supplier to run on null or empty lists.
     * @return The reducer decorator.
     */
    default Reducer<T> orElse(Supplier<T> onNullOrEmpty) {
        return orElse(this, onNullOrEmpty);
    }

    /**
     * Returns a decorator applying reducer to non-empty lists, or returning null on null or empty lists.
     * @return The reducer decorator.
     */
    default Reducer<T> orElseNull() {
        return orElseNull(this);
    }

    /**
     * Returns a decorator applying reducer to non-empty lists, or returning the supplier result on null or empty lists.
     * @param reducer The reducer.
     * @param onNullOrEmpty The supplier to run on null or empty lists.
     * @param <T> The items type.
     * @return The reducer decorator.
     */
    static <T> Reducer<T> orElse(Reducer<T> reducer, Supplier<T> onNullOrEmpty) {
        return items -> items == null || items.isEmpty() ? onNullOrEmpty.get() : reducer.apply(items);
    }

    /**
     * Returns a decorator applying reducer to non-empty lists, or returning null on null or empty lists. Equivalent to:
     * <pre>
     * Reducer.orElse(reducer, () -> null)
     * </pre>
     * @param reducer The reducer.
     * @param <T> The items type.
     * @return The reducer decorator.
     */
    static <T> Reducer<T> orElseNull(Reducer<T> reducer) {
        return orElse(reducer, () -> null);
    }

    /**
     * Returns a reducer applying a binary operator to non-empty lists as follows: If only one item in the list, it is
     * returned as is, else applied on by the operator with the second item, and the result is applied on by the
     * operator with subsequent items. In other words, with operator applying to items as (#, #): (((0, 1), 2), 3)...
     * @param operator The operator.
     * @param <T> The items type.
     * @return The reducer.
     */
    static <T> Reducer<T> from(BinaryOperator<T> operator) {
        return items -> {
            var iterator = Sugar.requireNonEmpty(items).iterator();
            T result = iterator.next();
            while (iterator.hasNext())
                result = operator.apply(result, iterator.next());
            return result;
        };
    }

    /**
     * Returns a reducer returning the first item of non-empty lists, or else null. Equivalent to:
     * <pre>
     * Reducer.orElseNull(Sugar::first)
     * </pre>
     * @param <T> The items type.
     * @return The reducer.
     */
    static <T> Reducer<T> first() {
        return orElseNull(Sugar::first);
    }

    /**
     * Returns a reducer returning the last item of non-empty lists, or else null. Equivalent to:
     * <pre>
     * Reducer.orElseNull(Sugar::last)
     * </pre>
     * @param <T> The items type.
     * @return The reducer.
     */
    static <T> Reducer<T> last() {
        return orElseNull(Sugar::last);
    }

    /**
     * Returns a reducer returning the maximum value in non-empty lists.
     * @param comparator The comparator.
     * @param <T> The items type.
     * @return The reducer.
     */
    static <T> Reducer<T> max(Comparator<T> comparator) {
        return from(BinaryOperator.maxBy(comparator));
    }

    /**
     * Returns a reducer returning the maximum value in non-empty lists.
     * @param <T> The items type.
     * @return The reducer.
     */
    static <T extends Comparable<T>> Reducer<T> max() {
        return max(Comparable::compareTo);
    }

    /**
     * Returns a reducer returning the minimum value in non-empty lists.
     * @param comparator The comparator.
     * @param <T> The items type.
     * @return The reducer.
     */
    static <T> Reducer<T> min(Comparator<T> comparator) {
        return from(BinaryOperator.minBy(comparator));
    }

    /**
     * Returns a reducer returning the maximum value in non-empty lists.
     * @param <T> The items type.
     * @return The reducer.
     */
    static <T extends Comparable<T>> Reducer<T> min() {
        return min(Comparable::compareTo);
    }

    /**
     * Returns an exceptions reducer, returning the first exception with subsequent ones as suppressed, or else null.
     */
    static Reducer<Exception> suppressor() {
        return orElseNull(exceptions -> {
            Exception main = Sugar.removeFirst(Sugar.requireNoneNull(exceptions));
            exceptions.forEach(main::addSuppressed);
            return main;
        });
    }
}
