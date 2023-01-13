package lab.drop.swing;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;

/**
 * Utility methods for manually manipulating components' size and location in a layout-less parent.
 */
public abstract class ManualLayout {

    private ManualLayout() {}

    /**
     * Registers a manual layout operation for a parent's resize events.
     * @param parent The parent component.
     * @param manualLayout The manual layout operation.
     */
    public static void registerLayout(Component parent, Consumer<ComponentEvent> manualLayout) {
        parent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                manualLayout.accept(e);
            }
        });
    }

    public static void putInTopLeftCorner(Component component, int margin) {
        component.setLocation(margin, margin);
    }

    public static void putInTopRightCornerIn(Component parent, Component component, int margin) {
        component.setLocation(parent.getWidth() - component.getWidth() - margin, margin);
    }

    public static void putInBottomLeftCornerIn(Component parent, Component component, int margin) {
        component.setLocation(margin, parent.getHeight() - component.getHeight() - margin);
    }

    public static void putInBottomRightCornerIn(Component parent, Component component, int margin) {
        component.setLocation(parent.getWidth() - component.getWidth() - margin, parent.getHeight() -
                component.getHeight() - margin);
    }

    public static void stretchRightIn(Component parent, Component component, int margin) {
        component.setSize(parent.getWidth() - component.getX() - margin, component.getHeight());
    }

    public static void stretchDownIn(Component parent, Component component, int margin) {
        component.setSize(component.getWidth(), parent.getHeight() - component.getY() - margin);
    }

    public static void putBelow(Component reference, Component component, int margin) {
        component.setLocation(reference.getX(), reference.getY() + reference.getHeight() + margin);
    }

    public static void putAbove(Component reference, Component component, int margin) {
        component.setLocation(reference.getX(), reference.getY() - component.getHeight() - margin);
    }

    public static void putToTheRightOf(Component reference, Component component, int margin) {
        component.setLocation(reference.getX() + reference.getWidth() + margin, reference.getY());
    }

    public static void putToTheLeftOf(Component reference, Component component, int margin) {
        component.setLocation(reference.getX() - component.getWidth() - margin, reference.getY());
    }

    public static void stretchDownTowards(Component reference, Component component, int margin) {
        component.setSize(component.getWidth(), reference.getY() - component.getY() - margin);
    }

    public static void stretchRightTowards(Component reference, Component component, int margin) {
        component.setSize(reference.getX() - component.getX() - margin, component.getY());
    }

    public static void resizeCentered(Component component, int width, int height) {
        component.setBounds(component.getX() + (component.getWidth() - width) / 2, component.getY() +
                (component.getHeight() - height) / 2, width, height);
    }
}
