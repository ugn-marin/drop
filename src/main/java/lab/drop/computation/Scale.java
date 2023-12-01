package lab.drop.computation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.UnaryOperator;

/**
 * A function modifying double values to a defined scale.
 */
public record Scale(int scale) implements UnaryOperator<Double> {
    private static final Scale defaultScale = new Scale(2);

    /**
     * Returns the default scale function instance with a scale of 2.
     */
    public static Scale getDefault() {
        return defaultScale;
    }

    /**
     * Applies the scale.
     * @param n The value.
     * @return If <code>n</code> is null, infinite or NaN, returned as is. Else, returns a new double with the value of
     * <code>n</code> with the defined scale.
     */
    @Override
    public Double apply(Double n) {
        return n == null || n.isInfinite() || n.isNaN() ? n : (Double) BigDecimal.valueOf(n).setScale(scale,
                RoundingMode.HALF_UP).doubleValue();
    }
}
