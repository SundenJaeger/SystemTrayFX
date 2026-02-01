package com.systemtray.core;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class TrayMenu extends TrayMenuItem {
    private Display display;
    private Menu subMenu;
    private SystemTrayFX ctx;

    private final ObservableList<TrayMenuItem> items = FXCollections.observableArrayList();

    /* ---------------- Constructors ---------------- */

    public TrayMenu() {
        this("Menu", null);
    }

    public TrayMenu(TrayMenuItem... items) {
        this("Menu", null, items);
    }

    public TrayMenu(String text) {
        this(text, null);
    }

    public TrayMenu(String text, Image image) {
        this(text, image, (TrayMenuItem[]) null);
    }

    public TrayMenu(String text, Image image, TrayMenuItem... items) {
        super(text, image);

        if (items != null) {
            this.items.addAll(items);
        }

        this.items.addListener((ListChangeListener<TrayMenuItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    display.asyncExec(() -> change.getAddedSubList()
                            .forEach(trayMenuItem -> trayMenuItem.create(display, subMenu, ctx)));
                }
            }
        });
    }

    /* ---------------- Getters/Setters ---------------- */

    public ObservableList<TrayMenuItem> getItems() {
        return items;
    }

    /* ---------------- Protected Methods ---------------- */

    @Override
    protected int getSWTStyle() {
        return SWT.CASCADE;
    }

    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        this.display = display;
        this.ctx = ctx;

        MenuItem root = new MenuItem(menu, SWT.CASCADE);

        applyInitialState(root, ctx);
        installBaseListeners(display, root, ctx);

        subMenu = new Menu(menu);
        root.setMenu(subMenu);

        items.forEach(trayMenuItem -> trayMenuItem.create(display, subMenu, ctx));
    }
}
