package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MenuItem;

public class TrayCheckMenuItem extends TrayMenuItem {
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    /* ---------------- Constructors ---------------- */

    public TrayCheckMenuItem() {
        this("Check Item", null);
    }

    public TrayCheckMenuItem(String text) {
        this(text, null);
    }

    public TrayCheckMenuItem(String text, Image image) {
        super(text, image);
    }

    /* ---------------- Getters/Setters ---------------- */

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    /* ---------------- Properties ---------------- */

    public BooleanProperty selectedProperty() {
        return selected;
    }

    /* ---------------- Protected Methods ---------------- */

    @Override
    protected int getSWTStyle() {
        return SWT.CHECK;
    }

    @Override
    protected void installSubclassListeners(Display display, MenuItem menuItem, SystemTrayFX ctx) {
        menuItem.setSelection(selected.get());

        selected.addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setSelection(newValue));
            }
        });

        menuItem.addListener(SWT.Selection, event -> {
            if (!menuItem.isDisposed()) {
                boolean newValue = menuItem.getSelection();

                if (!selected.isBound()) {
                    Platform.runLater(() -> selected.set(newValue));
                }
            }
        });
    }
}
