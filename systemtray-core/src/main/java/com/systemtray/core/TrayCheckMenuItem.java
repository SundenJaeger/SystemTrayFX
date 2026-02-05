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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MenuItem;

/**
 * A menu item with a checkbox that can be toggled on or off.
 *
 * <p>This menu item displays a checkmark when selected and no checkmark when
 * deselected. The selected state is synchronized bidirectionally between the
 * JavaFX property and the native system tray widget.
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Simple checkbox item
 * TrayCheckMenuItem darkMode = new TrayCheckMenuItem("Dark Mode");
 * darkMode.setOnAction(e -> toggleDarkMode());
 *
 * // Checkbox with initial state
 * TrayCheckMenuItem autoStart = new TrayCheckMenuItem("Start on Login");
 * autoStart.setSelected(true);
 *
 * // Checkbox bound to application state
 * TrayCheckMenuItem notifications = new TrayCheckMenuItem("Enable Notifications");
 * notifications.selectedProperty().bindBidirectional(notificationsEnabledProperty);
 *
 * // Checkbox with custom action based on state
 * TrayCheckMenuItem item = new TrayCheckMenuItem("Feature");
 * item.setOnAction(e -> {
 *     if (item.isSelected()) {
 *         enableFeature();
 *     } else {
 *         disableFeature();
 *     }
 * });
 * }</pre>
 *
 * @see TrayMenuItem
 * @see TrayExitMenuItem
 * @see FXMenuItemWrapper
 * @see TrayMenu
 * @see Separator
 */
public class TrayCheckMenuItem extends TrayMenuItem {

    /* ---------------- Constants ---------------- */

    /**
     * Fallback text used when a menu item text is null, empty, or blank.
     * Prevents tray items from rendering with an invisible label
     */
    private static final String DEFAULT_CHECK_TEXT = "Check Item";

    /* ---------------- Constructors ---------------- */

    /**
     * Creates a checkbox menu item with default text "Check Item" and no icon.
     */
    public TrayCheckMenuItem() {
        this(DEFAULT_CHECK_TEXT, null);
    }

    /**
     * Creates a checkbox menu item with the specified text and no icon.
     *
     * @param text the text to display
     */
    public TrayCheckMenuItem(String text) {
        this(text, null);
    }

    /**
     * Creates a checkbox menu item with the specified text and icon.
     *
     * @param text  the text to display
     * @param image the icon to display
     */
    public TrayCheckMenuItem(String text, Image image) {
        super(Utils.safeText(DEFAULT_CHECK_TEXT, text), image);
    }

    /* ---------------- Getters/Setters ---------------- */

    /**
     * Checks if the checkbox is currently selected.
     *
     * @return true if selected (checked), false if deselected (unchecked)
     */
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * Sets the selected state of the checkbox.
     *
     * @param selected true to select (check), false to deselect (uncheck)
     */
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    @Override
    public void setText(String text) {
        super.setText(Utils.safeText(DEFAULT_CHECK_TEXT, text));
    }

    /* ---------------- Properties ---------------- */

    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    /**
     * Returns the selected property for binding.
     *
     * <p>This property can be bound to application state for automatic
     * synchronization:
     * <pre>{@code
     * checkItem.selectedProperty().bindBidirectional(myBooleanProperty);
     * }</pre>
     *
     * @return the selected property
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /* ---------------- Protected Methods ---------------- */

    /**
     * Returns the SWT style flags for a checkbox menu item.
     *
     * @return SWT.CHECK style constant
     */
    @Override
    protected int getSWTStyle() {
        return SWT.CHECK;
    }

    /**
     * Installs listeners to synchronize the checkbox state between JavaFX and SWT.
     *
     * <p>This creates a bidirectional binding:
     * <ul>
     *   <li>Changes to the JavaFX property update the SWT widget</li>
     *   <li>User clicks on the SWT widget update the JavaFX property</li>
     * </ul>
     *
     * @param display  the SWT display
     * @param menuItem the SWT menu item
     * @param ctx      the system tray context
     */
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
