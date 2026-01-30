package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

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

    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        org.eclipse.swt.widgets.MenuItem menuItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.CHECK);
        menuItem.setText(getText());

        if (getImage() != null) {
            menuItem.setImage(ctx.createImage(Utils.toSWTImage(getImage())));
        }

        menuItem.setSelection(selected.get());
        menuItem.setEnabled(!isDisabled());

        if (getOnAction() != null) {
            menuItem.addListener(SWT.Selection, event -> Platform.runLater(() -> getOnAction().handle(new ActionEvent(this, null))));
        }

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

        textProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setText(newValue));
            }
        });

        disableProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setEnabled(!newValue));
            }
        });

        imageProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> ctx.createImage(Utils.toSWTImage(newValue)));
            }
        });
    }
}
