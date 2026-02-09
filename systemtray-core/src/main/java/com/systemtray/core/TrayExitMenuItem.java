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
import javafx.scene.image.Image;

/**
 * A specialized tray menu item that exits the application and properly cleans up
 * system tray resources when clicked.
 *
 * <p>This menu item automatically configures its action to:
 * <ol>
 *   <li>Dispose of the system tray resources</li>
 *   <li>Exit the JavaFX application via {@link Platform#exit()}</li>
 * </ol>
 *
 * <p><strong>Usage Example with getItems():</strong>
 * <pre>{@code
 * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, true);
 *
 * // Add exit menu item using observable list
 * tray.getItems().add(new TrayExitMenuItem());
 *
 * // Or with custom text
 * tray.getItems().add(new TrayExitMenuItem("Quit Application"));
 *
 * // Or with custom text and icon
 * tray.getItems().add(new TrayExitMenuItem("Exit", exitIcon));
 * }</pre>
 *
 * <p><strong>Usage Example with addEntry():</strong>
 * <pre>{@code
 * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, true);
 *
 * // Add exit menu item using addEntry method
 * tray.addEntry(new TrayExitMenuItem());
 *
 * // Or with custom text
 * tray.addEntry(new TrayExitMenuItem("Quit Application"));
 *
 * // Or with custom text and icon
 * tray.addEntry(new TrayExitMenuItem("Exit", exitIcon));
 * }</pre>
 *
 * <p><strong>Important:</strong> This menu item is especially important when using
 * {@code isMinimizeToTray = true} in {@link SystemTrayFX}, as it provides a way for
 * users to properly exit the application and ensures all resources are cleaned up.
 *
 * @see TrayMenuItem
 * @see TrayCheckMenuItem
 * @see FXMenuItemWrapper
 * @see TrayMenu
 * @see Separator
 * @see SystemTrayFX
 */
public class TrayExitMenuItem extends TrayMenuItem {

    /* ---------------- Constants ---------------- */

    /**
     * Fallback text used when a menu item text is null, empty, or blank.
     * Prevents tray items from rendering with an invisible label
     */
    private static final String DEFAULT_EXIT_TEXT = "Exit";

    /* ---------------- Constructors ---------------- */

    /**
     * Creates an exit menu item with default text "Exit" and no icon.
     *
     * <p>The action is automatically configured to dispose of system tray resources
     * and exit the JavaFX application.
     */
    public TrayExitMenuItem() {
        this(DEFAULT_EXIT_TEXT);
    }

    /**
     * Creates an exit menu item with custom text and no icon.
     *
     * <p>The action is automatically configured to dispose of system tray resources
     * and exit the JavaFX application.
     *
     * @param text the text to display on the menu item (e.g., "Exit", "Quit", "Close")
     */
    public TrayExitMenuItem(String text) {
        this(text, null);
    }

    /**
     * Creates an exit menu item with custom text and icon.
     *
     * <p>The action is automatically configured to dispose of system tray resources
     * and exit the JavaFX application.
     *
     * @param text  the text to display on the menu item
     * @param image the icon to display next to the text (may be null for no icon)
     */
    public TrayExitMenuItem(String text, Image image) {
        super(Utils.safeText(DEFAULT_EXIT_TEXT, text), image);
        setOnAction(event -> {
            display.asyncExec(this::dispose);
            Platform.exit();
        });
    }

    @Override
    public void setText(String text) {
        super.setText(Utils.safeText(DEFAULT_EXIT_TEXT, text));
    }

    @Override
    protected void dispose() {
        ctx.dispose();
    }
}
