package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class TrayMenuItem {
    private final StringProperty text = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final BooleanProperty disable = new SimpleBooleanProperty(false);

    private ObjectProperty<EventHandler<ActionEvent>> onAction;
    private SystemTrayFX ctx;

    /* ---------------- Constructors ---------------- */

    public TrayMenuItem() {
        this("Item", null);
    }

    public TrayMenuItem(String text) {
        this(text, null);
    }

    public TrayMenuItem(String text, Image image) {
        this.text.set(text);
        this.image.set(image);
    }

    /* ---------------- Getters/Setters ---------------- */

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public Image getImage() {
        return image.get();
    }

    public void setImage(Image image) {
        this.image.set(image);
    }

    public boolean isDisabled() {
        return disable.get();
    }

    public void setDisable(boolean isDisabled) {
        disable.set(isDisabled);
    }

    public void setOnAction(EventHandler<ActionEvent> action) {
        onActionProperty().set(action);
    }

    public EventHandler<ActionEvent> getOnAction() {
        return onAction == null ? null : onAction.get();
    }

    /* ---------------- Properties ---------------- */

    public StringProperty textProperty() {
        return text;
    }

    public ObjectProperty<Image> imageProperty() {
        return image;
    }

    public BooleanProperty disableProperty() {
        return disable;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        if (onAction == null) {
            onAction = new ObjectPropertyBase<>() {
                @Override
                public Object getBean() {
                    return TrayMenuItem.this;
                }

                @Override
                public String getName() {
                    return "onAction";
                }
            };
        }
        return onAction;
    }

    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        this.ctx = ctx;

        org.eclipse.swt.widgets.MenuItem menuItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
        menuItem.setText(text.get());

        if (image.get() != null) {
            menuItem.setImage(ctx.createImage(Utils.toSWTImage(image.get())));
        }

        menuItem.setEnabled(!disable.get());

        if (getOnAction() != null) {
            menuItem.addListener(SWT.Selection, event -> Platform.runLater(() -> getOnAction().handle(new ActionEvent(this, null))));
        }

        text.addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setText(newValue));
            }
        });

        disable.addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setEnabled(!newValue));
            }
        });

        image.addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> ctx.createImage(Utils.toSWTImage(newValue)));
            }
        });
    }

    protected void dispose() {
        ctx.dispose();
    }
}
