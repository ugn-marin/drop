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

    /**
     * Positions a component in the top-left corner of the parent with a given margin.
     * @param component The component to position.
     * @param margin The margin from the top-left corner.
     */
    public static void putInTopLeftCorner(Component component, int margin) {
        component.setLocation(margin, margin);
    }

    /**
     * Positions a component in the top-right corner of a parent with a given margin.
     * @param parent The parent component.
     * @param component The component to position.
     * @param margin The margin from the top-right corner.
     */
    public static void putInTopRightCornerIn(Component parent, Component component, int margin) {
        component.setLocation(parent.getWidth() - component.getWidth() - margin, margin);
    }

    /**
     * Positions a component in the bottom-left corner of a parent with a given margin.
     * @param parent The parent component.
     * @param component The component to position.
     * @param margin The margin from the bottom-left corner.
     */
    public static void putInBottomLeftCornerIn(Component parent, Component component, int margin) {
        component.setLocation(margin, parent.getHeight() - component.getHeight() - margin);
    }

    /**
     * Positions a component in the bottom-right corner of a parent with a given margin.
     * @param parent The parent component.
     * @param component The component to position.
     * @param margin The margin from the bottom-right corner.
     */
    public static void putInBottomRightCornerIn(Component parent, Component component, int margin) {
        component.setLocation(parent.getWidth() - component.getWidth() - margin, parent.getHeight() -
                component.getHeight() - margin);
    }

    /**
     * Stretches a component to the right of a parent with a given margin.
     * @param parent The parent component.
     * @param component The component to stretch.
     * @param margin The margin from the right edge of the parent.
     */
    public static void stretchRightIn(Component parent, Component component, int margin) {
        component.setSize(parent.getWidth() - component.getX() - margin, component.getHeight());
    }

    /**
     * Stretches a component down in a parent with a given margin.
     * @param parent The parent component.
     * @param component The component to stretch.
     * @param margin The margin from the bottom edge of the parent.
     */
    public static void stretchDownIn(Component parent, Component component, int margin) {
        component.setSize(component.getWidth(), parent.getHeight() - component.getY() - margin);
    }

    /**
     * Puts a component below a reference component with a given margin.
     * @param reference The reference component.
     * @param component The component to position.
     * @param margin The margin from the bottom of the reference component.
     */
    public static void putBelow(Component reference, Component component, int margin) {
        component.setLocation(reference.getX(), reference.getY() + reference.getHeight() + margin);
    }

    /**
     * Puts a component above a reference component with a given margin.
     * @param reference The reference component.
     * @param component The component to position.
     * @param margin The margin from the top of the reference component.
     */
    public static void putAbove(Component reference, Component component, int margin) {
        component.setLocation(reference.getX(), reference.getY() - component.getHeight() - margin);
    }

    /**
     * Puts a component to the right of a reference component with a given margin.
     * @param reference The reference component.
     * @param component The component to position.
     * @param margin The margin from the right of the reference component.
     */
    public static void putToTheRightOf(Component reference, Component component, int margin) {
        component.setLocation(reference.getX() + reference.getWidth() + margin, reference.getY());
    }

    /**
     * Puts a component to the left of a reference component with a given margin.
     * @param reference The reference component.
     * @param component The component to position.
     * @param margin The margin from the left of the reference component.
     */
    public static void putToTheLeftOf(Component reference, Component component, int margin) {
        component.setLocation(reference.getX() - component.getWidth() - margin, reference.getY());
    }

    /**
     * Stretches a component down towards a reference component with a given margin.
     * @param reference The reference component.
     * @param component The component to stretch.
     * @param margin The margin from the top of the reference component.
     */
    public static void stretchDownTowards(Component reference, Component component, int margin) {
        component.setSize(component.getWidth(), reference.getY() - component.getY() - margin);
    }

    /**
     * Stretches a component right towards a reference component with a given margin.
     * @param reference The reference component.
     * @param component The component to stretch.
     * @param margin The margin from the left of the reference component.
     */
    public static void stretchRightTowards(Component reference, Component component, int margin) {
        component.setSize(reference.getX() - component.getX() - margin, component.getHeight());
    }

    /**
     * Resizes a component to given width and height, keeping it centered.
     * @param component The component to resize.
     * @param width The new width of the component.
     * @param height The new height of the component.
     */
    public static void resizeCentered(Component component, int width, int height) {
        component.setBounds(component.getX() + (component.getWidth() - width) / 2, component.getY() +
                (component.getHeight() - height) / 2, width, height);
    }
}
