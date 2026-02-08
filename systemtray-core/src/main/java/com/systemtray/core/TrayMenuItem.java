/*
 * Copyright (C) 2026 Rentoki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Base class for all system tray menu items.
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Simple menu item
 * TrayMenuItem item = new TrayMenuItem("Open");
 * item.setOnAction(e -> openApplication());
 *
 * // Menu item with icon
 * Image icon = new Image("/icons/open.png");
 * TrayMenuItem item = new TrayMenuItem("Open", icon);
 *
 * // Menu item with dynamic text
 * TrayMenuItem statusItem = new TrayMenuItem("Status: Idle");
 * statusItem.textProperty().bind(statusProperty);
 *
 * // Disabled menu item
 * TrayMenuItem item = new TrayMenuItem("Save");
 * item.setDisable(true);
 * item.disableProperty().bind(hasUnsavedChanges.not());
 * }</pre>
 *
 * @see TrayCheckMenuItem
 * @see TrayMenu
 * @see Separator
 * @see TrayExitMenuItem
 * @see FXMenuItemWrapper
 */
public class TrayMenuItem {

    /* ---------------- Constants ---------------- */

    /**
     * Fallback text used when a menu item text is null, empty, or blank.
     * Prevents tray items from rendering with an invisible label
     */
    private static final String DEFAULT_TEXT = "Item";

    /* ---------------- SWT Components ---------------- */

    /**
     * Reference to the parent system tray context
     */
    private SystemTrayFX ctx;

    private Listener selectionListener;

    /* ---------------- Constructors ---------------- */

    /**
     * Creates a menu item with default text "Item" and no icon.
     */
    public TrayMenuItem() {
        this(DEFAULT_TEXT, null);
    }

    /**
     * Creates a menu item with the specified text and no icon.
     *
     * @param text the text to display
     */
    public TrayMenuItem(String text) {
        this(text, null);
    }

    /**
     * Creates a menu item with the specified text and icon.
     *
     * @param text  the text to display
     * @param image the icon to display
     */
    public TrayMenuItem(String text, Image image) {
        setText(text);
        this.image.set(image);
    }

    /* ---------------- Getters/Setters ---------------- */

    /**
     * Gets the text displayed on the menu item.
     *
     * @return the menu item text
     */
    public String getText() {
        return text.get();
    }

    /**
     * Sets the text displayed on the menu item.
     *
     * @param text the new text
     */
    public void setText(String text) {
        this.text.set(Utils.safeText(DEFAULT_TEXT, text));
    }

    /**
     * Gets the icon image displayed on the menu item.
     *
     * @return the icon image, or null if no icon is set
     */
    public Image getImage() {
        return image.get();
    }

    /**
     * Sets the icon image displayed on the menu item.
     *
     * @param image the new icon image (may be null to remove icon)
     */
    public void setImage(Image image) {
        this.image.set(image);
    }

    /**
     * Checks if the menu item is disabled.
     *
     * @return true if disabled, false if enabled
     */
    public boolean isDisabled() {
        return disable.get();
    }

    /**
     * Sets the disabled state of the menu item.
     *
     * @param isDisabled true to disable, false to enable
     */
    public void setDisable(boolean isDisabled) {
        disable.set(isDisabled);
    }

    /**
     * Sets the action to execute when the menu item is clicked.
     * The action runs on the JavaFX application thread.
     *
     * @param action the action handler
     */
    public void setOnAction(EventHandler<ActionEvent> action) {
        onActionProperty().set(action);
    }

    /**
     * Gets the action event handler for this menu item.
     *
     * @return the action handler, or null if none is set
     */
    public EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }

    /* ---------------- Properties ---------------- */

    private final StringProperty text = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    private final BooleanProperty disable = new SimpleBooleanProperty(false);
    private final ObjectProperty<EventHandler<ActionEvent>> onAction = new SimpleObjectProperty<>();

    /**
     * Returns the text property for binding.
     *
     * @return the text property
     */
    public StringProperty textProperty() {
        return text;
    }

    /**
     * Returns the image property for binding.
     *
     * @return the image property
     */
    public ObjectProperty<Image> imageProperty() {
        return image;
    }

    /**
     * Returns the disable property for binding.
     *
     * @return the disable property
     */
    public BooleanProperty disableProperty() {
        return disable;
    }

    /**
     * Returns the onAction property for binding.
     *
     * @return the onAction property
     */
    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    /* ---------------- Protected Methods ---------------- */

    /**
     * Creates the SWT menu item and attaches it to the specified menu.
     * This method is called internally by the system tray framework.
     *
     * @param display the SWT display
     * @param menu    the parent menu
     * @param ctx     the system tray context
     */
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        this.ctx = ctx;

        org.eclipse.swt.widgets.MenuItem menuItem = new org.eclipse.swt.widgets.MenuItem(menu, getSWTStyle());
        applyInitialState(menuItem, ctx);
        installBaseListeners(display, menuItem, ctx);
        installSubclassListeners(display, menuItem, ctx);
    }

    /**
     * Returns the SWT style flags for this menu item.
     * Subclasses override this to provide different item types.
     *
     * @return SWT style constant (e.g., SWT.PUSH, SWT.CHECK, SWT.CASCADE)
     */
    protected int getSWTStyle() {
        return SWT.PUSH;
    }

    /**
     * Applies the initial state (text, icon, enabled) to the SWT menu item.
     *
     * @param menuItem the SWT menu item
     * @param ctx      the system tray context
     */
    protected void applyInitialState(MenuItem menuItem, SystemTrayFX ctx) {
        menuItem.setText(Utils.safeText(DEFAULT_TEXT, getText()));

        selectionListener = event -> {
            if (getOnAction() != null) {
                Platform.runLater(() -> getOnAction().handle(new ActionEvent(this, null)));
            }
        };
        menuItem.addListener(SWT.Selection, selectionListener);

        if (getImage() != null) {
            menuItem.setImage(ctx.createImage(Utils.toSWTImage(getImage())));
        }
        menuItem.setEnabled(!isDisabled());
    }

    /**
     * Installs listeners to synchronize JavaFX properties with the SWT widget.
     * This ensures changes to JavaFX properties are reflected in the system tray.
     *
     * @param display  the SWT display
     * @param menuItem the SWT menu item
     * @param ctx      the system tray context
     */
    protected void installBaseListeners(Display display, MenuItem menuItem, SystemTrayFX ctx) {
        text.addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setText(Utils.safeText(DEFAULT_TEXT, newValue)));
            }
        });

        onAction.addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            display.asyncExec(() -> {
                if (menuItem.isDisposed()) return;

                if (selectionListener != null) {
                    menuItem.removeListener(SWT.Selection, selectionListener);
                }

                if (newValue != null) {
                    selectionListener = event -> Platform.runLater(() -> newValue.handle(new ActionEvent(this, null)));
                    menuItem.addListener(SWT.Selection, selectionListener);
                }
            });
        });

        disable.addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setEnabled(!newValue));
            }
        });

            image.addListener((observable, oldValue, newValue) -> {
                if (display == null || display.isDisposed()) return;

                display.asyncExec(() -> {
                    if (menuItem.isDisposed()) return;

                    if (newValue != null) {
                        menuItem.setImage(ctx.createImage(Utils.toSWTImage(newValue)));
                    }
                });
            });
    }

    /**
     * Hook for subclasses to install additional listeners.
     * Called after base listeners are installed.
     *
     * @param display  the SWT display
     * @param menuItem the SWT menu item
     * @param ctx      the system tray context
     */
    protected void installSubclassListeners(
            Display display,
            org.eclipse.swt.widgets.MenuItem menuItem,
            SystemTrayFX ctx) {
        // subclasses override
    }

    /**
     * Disposes of this menu item and its resources.
     * Called when the item is removed from the menu.
     */
    protected void dispose() {
        ctx.dispose();
    }
}
