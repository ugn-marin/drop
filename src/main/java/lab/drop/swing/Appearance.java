package lab.drop.swing;

import lab.drop.Sugar;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * Various look and feel utilities.
 */
public class Appearance {

    private Appearance() {}

    /**
     * Applies the Nimbus look and feel.
     */
    public static void setNimbus() {
        setNimbus(null);
    }

    /**
     * Applies the Nimbus look and feel with a base color override.
     * @param base The base color of Nimbus objects.
     */
    public static void setNimbus(Color base) {
        setNimbus(base, null, null, null);
    }

    /**
     * Applies a light Nimbus look and feel with a base and focus colors override.
     * @param base The base color of Nimbus objects.
     * @param focus The focus color, mostly affects the focus glow, text selection and progress color.
     */
    public static void setNimbusLight(Color base, Color focus) {
        setNimbus(base, focus, Color.darkGray, mix(Color.white, Color.lightGray, 0.1D));
    }

    /**
     * Applies a dark Nimbus look and feel with a base and focus colors override.
     * @param base The base color of Nimbus objects.
     * @param focus The focus color, mostly affects the focus glow, text selection and progress color.
     */
    public static void setNimbusDark(Color base, Color focus) {
        setNimbus(base, focus, Color.white, mix(Color.darkGray, Color.black, 0.1D));
    }

    /**
     * Applies a white Nimbus look and feel.
     */
    public static void setNimbusWhite() {
        setNimbusWhite(null);
    }

    /**
     * Applies a white Nimbus look and feel with a focus color override.
     * @param focus The focus color, mostly affects the focus glow, text selection and progress color.
     */
    public static void setNimbusWhite(Color focus) {
        setNimbusLight(Color.lightGray, focus);
    }

    /**
     * Applies a black Nimbus look and feel.
     */
    public static void setNimbusBlack() {
        setNimbusBlack(null);
    }

    /**
     * Applies a black Nimbus look and feel with a focus color override.
     * @param focus The focus color, mostly affects the focus glow, text selection and progress color.
     */
    public static void setNimbusBlack(Color focus) {
        setNimbusDark(Color.black, focus);
    }

    /**
     * Applies a dark blueish Nimbus look and feel.
     */
    public static void setNimbusDarkBlueish() {
        setNimbusDarkTint(mix(Color.blue, Color.green, 0.2D));
    }

    /**
     * Applies a dark Nimbus look and feel with a global tint.
     */
    public static void setNimbusDarkTint(Color tint) {
        setNimbus(mix(mix(tint, Color.darkGray, 0.7D).darker(), Color.black, 0.4D),
                mix(tint, Color.gray, 0.4D).brighter(),
                mix(tint, Color.lightGray, 0.9D).brighter(),
                mix(tint.darker(), Color.darkGray, 0.9D));
    }

    /**
     * Applies the Nimbus look and feel, with optional color modifications. Null colors are ignored (remain default).
     * @param base The base color of Nimbus objects.
     * @param focus The focus color, mostly affects the focus glow, text selection and progress color.
     * @param foreground The default text color.
     * @param background The default background and control color.
     */
    public static void setNimbus(Color base, Color focus, Color foreground, Color background) {
        for (var info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                Sugar.sneaky(() -> UIManager.setLookAndFeel(info.getClassName()));
                if (base != null) {
                    UIManager.put("nimbusBase", base);
                    UIManager.put("nimbusBlueGrey", mix(base.brighter(), Color.lightGray, 0.45D));
                }
                if (focus != null) {
                    UIManager.put("nimbusFocus", focus);
                    UIManager.put("nimbusOrange", focus.brighter());
                    UIManager.put("nimbusSelectionBackground", mix(focus.darker(), Color.darkGray, 0.7D));
                    UIManager.put("nimbusSelectedText", Color.white);
                }
                if (foreground != null) {
                    UIManager.put("text", foreground);
                    UIManager.put("nimbusDisabledText", mix(foreground, Color.gray, 0.3D));
                }
                if (background != null) {
                    UIManager.put("control", background);
                    UIManager.put("info", mix(background, Color.gray, 0.2D));
                    UIManager.put("nimbusLightBackground", background.brighter());
                }
                break;
            }
        }
    }

    /**
     * Sets the button background according to the base color of the Nimbus look and feel if applied.
     */
    public static void setButtonBaseColor(JButton button) {
        try {
            button.setBackground((Color) UIManager.get("nimbusBase"));
        } catch (Exception ignore) {}
    }

    /**
     * Constructs an equal mix color of the provided colors.
     */
    public static Color mix(Color... colors) {
        var color = Sugar.first(colors);
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
