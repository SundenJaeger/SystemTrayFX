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
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * A wrapper that adapts JavaFX menu items for use in the system tray.
 *
 * <p>This class allows you to reuse existing JavaFX menu items in your system tray
 * without recreating them. It automatically synchronizes properties between JavaFX
 * menu items and their SWT counterparts.
 *
 * <p><strong>Supported JavaFX Menu Item Types:</strong>
 * <ul>
 *   <li>{@link javafx.scene.control.MenuItem} - Regular clickable items</li>
 *   <li>{@link CheckMenuItem} - Checkbox items with selection state</li>
 *   <li>{@link SeparatorMenuItem} - Visual separators</li>
 *   <li>{@link javafx.scene.control.Menu} - Submenus with nested items</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Wrap existing JavaFX menu items
 * javafx.scene.control.MenuItem fxOpen = new javafx.scene.control.MenuItem("Open");
 * fxOpen.setOnAction(e -> openFile());
 *
 * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon);
 * tray.addEntry(new FXMenuItemWrapper(fxOpen));
 *
 * // Wrap a JavaFX checkbox item
 * CheckMenuItem fxDarkMode = new CheckMenuItem("Dark Mode");
 * fxDarkMode.selectedProperty().bindBidirectional(darkModeEnabledProperty);
 * tray.addEntry(new FXMenuItemWrapper(fxDarkMode));
 *
 * // Wrap an entire JavaFX menu (with submenu)
 * javafx.scene.control.Menu fxFileMenu = new javafx.scene.control.Menu("File");
 * fxFileMenu.getItems().addAll(
 *     new javafx.scene.control.MenuItem("New"),
 *     new javafx.scene.control.MenuItem("Open"),
 *     new javafx.scene.control.MenuItem("Save")
 * );
 * tray.addEntry(new FXMenuItemWrapper(fxFileMenu));
 *
 * // Share menu between application window and system tray
 * javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
 * javafx.scene.control.Menu fileMenu = new javafx.scene.control.Menu("File");
 * // ... add items to fileMenu ...
 * menuBar.getMenus().add(fileMenu);
 *
 * // Same menu in system tray
 * tray.addEntry(new FXMenuItemWrapper(fileMenu));
 * }</pre>
 *
 * <p><strong>Important Note:</strong>
 * <ul>
 *   <li>Icons are not currently synchronized from JavaFX to SWT</li>
 * </ul>
 *
 * @see TrayMenuItem
 * @see TrayCheckMenuItem
 * @see TrayExitMenuItem
 * @see TrayMenu
 * @see Separator
 */
public class FXMenuItemWrapper extends TrayMenuItem {

    /* ---------------- Fields ---------------- */

    /**
     * The wrapped JavaFX menu item
     */
    private final javafx.scene.control.MenuItem fxItem;

    /* ---------------- Constructors ---------------- */

    /**
     * Creates a wrapper for the specified JavaFX menu item.
     *
     * <p>The wrapper automatically determines the appropriate SWT style based on
     * the JavaFX item type and sets up bidirectional property synchronization.
     *
     * @param fxItem the JavaFX menu item to wrap
     * @throws NullPointerException if fxItem is null
     */
    public FXMenuItemWrapper(javafx.scene.control.MenuItem fxItem) {
        this.fxItem = fxItem;
    }

    /* ---------------- Protected Methods ---------------- */

    /**
     * Determines the appropriate SWT style based on the JavaFX item type.
     *
     * <p>Mapping:
     * <ul>
     *   <li>CheckMenuItem → SWT.CHECK</li>
     *   <li>SeparatorMenuItem → SWT.SEPARATOR</li>
     *   <li>Menu → SWT.CASCADE</li>
     *   <li>MenuItem → SWT.PUSH</li>
     * </ul>
     *
     * @return the corresponding SWT style constant
     */
    @Override
    protected int getSWTStyle() {
        if (fxItem instanceof CheckMenuItem) return SWT.CHECK;
        if (fxItem instanceof SeparatorMenuItem) return SWT.SEPARATOR;
        if (fxItem instanceof javafx.scene.control.Menu) return SWT.CASCADE;
        return SWT.PUSH;
    }

    /**
     * Creates the SWT menu item and sets up property synchronization.
     *
     * @param display the SWT display
     * @param menu    the parent menu
     * @param ctx     the system tray context
     */
    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        org.eclipse.swt.widgets.MenuItem menuItem = new MenuItem(menu, getSWTStyle());

        setText(fxItem.getText());
        setDisable(fxItem.isDisable());
        setOnAction(fxItem.getOnAction());

        applyInitialState(menuItem, ctx);
        installBaseListeners(display, menuItem, ctx);
        installSubclassListeners(display, menuItem, ctx);

        fxItem.textProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> setText(newValue));
            }
        });

        fxItem.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> setDisable(newValue));
            }
        });
    }

    /**
     * Installs type-specific listeners based on the JavaFX item type.
     *
     * @param display  the SWT display
     * @param menuItem the SWT menu item
     * @param ctx      the system tray context
     */
    @Override
    protected void installSubclassListeners(Display display, MenuItem menuItem, SystemTrayFX ctx) {
        if (fxItem instanceof CheckMenuItem checkItem) {
            menuItem.setSelection(checkItem.isSelected());

            checkItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (display == null || display.isDisposed()) return;

                if (!menuItem.isDisposed()) {
                    display.asyncExec(() -> menuItem.setSelection(newVal));
                }
            });

            menuItem.addListener(SWT.Selection, event -> {
                if (!menuItem.isDisposed()) {
                    boolean newValue = menuItem.getSelection();

                    if (!checkItem.selectedProperty().isBound()) {
                        Platform.runLater(() -> checkItem.selectedProperty().set(newValue));
                    }
                }
            });
        } else if (fxItem instanceof javafx.scene.control.Menu fxMenu) {
            org.eclipse.swt.widgets.Menu swtSubMenu = new org.eclipse.swt.widgets.Menu(menuItem);
            menuItem.setMenu(swtSubMenu);

            for (javafx.scene.control.MenuItem item : fxMenu.getItems()) {
                FXMenuItemWrapper wrapper = new FXMenuItemWrapper(item);
                wrapper.create(display, swtSubMenu, ctx);
            }

            fxMenu.getItems().addListener((javafx.collections.ListChangeListener<javafx.scene.control.MenuItem>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        if (display == null || display.isDisposed()) return;

                        display.asyncExec(() -> {
                            if (!swtSubMenu.isDisposed()) {
                                for (javafx.scene.control.MenuItem item : change.getAddedSubList()) {
                                    FXMenuItemWrapper wrapper = new FXMenuItemWrapper(item);
                                    wrapper.create(display, swtSubMenu, ctx);
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}
