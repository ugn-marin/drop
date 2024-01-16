package lab.drop.swing;

import lab.drop.data.Data;

import java.awt.*;
import java.util.function.Function;

/**
 * Various look and feel utilities.
 */
public class Appearance {

    private Appearance() {}

    /**
     * Constructs an equal mix color of the provided colors.
     */
    public static Color mix(Color... colors) {
        var color = Data.first(colors);
        for (int i = 1; i < colors.length; i++)
            color = mix(color, colors[i], 1D / (i + 1));
        return color;
    }

    /**
     * Constructs a mix color of the provided colors, according to the amount of the color to add.
     * @param base The base color.
     * @param add The color to add to base.
     * @param amount The proportional amount of the color to add (between 0 and 1).
     * @return The mixed color.
     */
    public static Color mix(Color base, Color add, double amount) {
        Function<Function<Color, Integer>, Integer> mixer = component ->
                (int) (((double) component.apply(base)) * (1D - amount) + ((double) component.apply(add)) * amount);
        return new Color(mixer.apply(Color::getRed), mixer.apply(Color::getGreen), mixer.apply(Color::getBlue),
                mixer.apply(Color::getAlpha));
    }

    /**
     * Sets rendering hints in the graphics object, like antialiasing, quality color rendering, bilinear interpolation,
     * sub-pixel accuracy of shapes and more.
     */
    public static void applyQualityRendering(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}
