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

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A menu item that contains a submenu with additional menu items.
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Simple submenu
 * TrayMenu fileMenu = new TrayMenu("File");
 * fileMenu.getItems().addAll(
 *     new TrayMenuItem("Open", e -> open()),
 *     new TrayMenuItem("Save", e -> save()),
 *     new Separator(),
 *     new TrayMenuItem("Exit", e -> exit())
 * );
 *
 * // Submenu with icon
 * Image icon = new Image("/icons/settings.png");
 * TrayMenu settingsMenu = new TrayMenu("Settings", icon);
 * settingsMenu.getItems().addAll(
 *     new TrayCheckMenuItem("Dark Mode"),
 *     new TrayCheckMenuItem("Auto-start")
 * );
 *
 * // Submenu with initial items (constructor)
 * TrayMenu viewMenu = new TrayMenu("View",
 *     new TrayCheckMenuItem("Show Toolbar"),
 *     new TrayCheckMenuItem("Show Status Bar")
 * );
 *
 * // Nested submenus
 * TrayMenu exportMenu = new TrayMenu("Export");
 * exportMenu.getItems().addAll(
 *     new TrayMenuItem("As PDF"),
 *     new TrayMenuItem("As HTML")
 * );
 *
 * TrayMenu fileMenu = new TrayMenu("File");
 * fileMenu.getItems().addAll(
 *     new TrayMenuItem("New"),
 *     new TrayMenuItem("Open"),
 *     exportMenu,  // Nested submenu
 *     new Separator(),
 *     new TrayExitMenuItem()
 * );
 * }</pre>
 *
 * @see TrayMenuItem
 * @see TrayCheckMenuItem
 * @see TrayExitMenuItem
 * @see FXMenuItemWrapper
 * @see Separator
 */
public class TrayMenu extends TrayMenuItem {

    /* ---------------- Constructors ---------------- */

    /** The SWT display for executing operations on the correct thread */
    private Display display;

    /** The SWT submenu containing child items */
    private Menu subMenu;

    /** Reference to the parent system tray context */
    private SystemTrayFX ctx;

    /* ---------------- Collections ---------------- */

    /** Observable list of items in the submenu */
    private final ObservableList<TrayMenuItem> items = FXCollections.observableArrayList();

    /** Queue for items added before the submenu is initialized */
    private final List<TrayMenuItem> pendingItems = new ArrayList<>();

    /* ---------------- States ---------------- */

    /** Flag indicating whether the submenu has been created */
    private boolean isInitialized = false;

    /* ---------------- Constructors ---------------- */

    /**
     * Creates an empty submenu with default text "Menu" and no icon.
     */
    public TrayMenu() {
        this("Menu", null);
    }

    /**
     * Creates a submenu with default text "Menu" and the specified initial items.
     *
     * @param items the initial items to add to the submenu
     */
    public TrayMenu(TrayMenuItem... items) {
        this("Menu", null, items);
    }

    /**
     * Creates an empty submenu with the specified text and no icon.
     *
     * @param text the text to display on the submenu item
     */
    public TrayMenu(String text) {
        this(text, null);
    }

    /**
     * Creates an empty submenu with the specified text and icon.
     *
     * @param text the text to display on the submenu item
     * @param image the icon to display
     */
    public TrayMenu(String text, Image image) {
        this(text, image, (TrayMenuItem[]) null);
    }

    /**
     * Creates a submenu with the specified text, icon, and initial items.
     *
     * @param text the text to display on the submenu item
     * @param image the icon to display
     * @param items the initial items to add to the submenu
     */
    public TrayMenu(String text, Image image, TrayMenuItem... items) {
        super(text, image);

        if (items != null) {
            this.items.addAll(items);
        }

        this.items.addListener((ListChangeListener<TrayMenuItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    List<TrayMenuItem> added = List.copyOf(change.getAddedSubList());

                    if (!isInitialized) {
                        pendingItems.addAll(added);
                    } else {
                        display.asyncExec(() -> added.forEach(item -> item.create(display, subMenu, ctx)));
                    }
                }

                if (change.wasRemoved()) {
                    List<TrayMenuItem> removed = List.copyOf(change.getRemoved());

                    if (isInitialized) {
                        display.asyncExec(() -> removed.forEach(TrayMenuItem::dispose));
                    }
                }
            }
        });
    }

    /* ---------------- Getters/Setters ---------------- */

    /**
     * Returns the observable list of submenu items.
     *
     * <p>Items can be added or removed from this list to update the submenu:
     * <pre>{@code
     * // Add items
     * menu.getItems().add(new TrayMenuItem("New Item"));
     *
     * // Add multiple items
     * menu.getItems().addAll(item1, item2, item3);
     *
     * // Remove items
     * menu.getItems().remove(item);
     *
     * // Clear all items
     * menu.getItems().clear();
     * }</pre>
     *
     * @return the observable list of menu items
     */
    public ObservableList<TrayMenuItem> getItems() {
        return items;
    }

    /* ---------------- Protected Methods ---------------- */

    /**
     * Returns the SWT style flags for a cascading menu item.
     *
     * @return SWT.CASCADE style constant
     */
    @Override
    protected int getSWTStyle() {
        return SWT.CASCADE;
    }

    /**
     * Creates the SWT submenu structure.
     *
     * <p>This method creates the menu item, attaches the submenu, and processes
     * any items that were added before initialization.
     *
     * @param display the SWT display
     * @param menu the parent menu
     * @param ctx the system tray context
     */
    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        this.display = display;
        this.ctx = ctx;

        MenuItem root = new MenuItem(menu, SWT.CASCADE);

        applyInitialState(root, ctx);
        installBaseListeners(display, root, ctx);

        subMenu = new Menu(menu);
        root.setMenu(subMenu);

        isInitialized = true;
        pendingItems.forEach(item -> item.create(display, subMenu, ctx));
        pendingItems.clear();
    }
}
