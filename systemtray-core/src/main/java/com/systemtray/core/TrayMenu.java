package com.systemtray.core;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class TrayMenu extends TrayMenuItem {
    private final ObservableList<TrayMenuItem> items = FXCollections.observableArrayList();

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
    }

    public ObservableList<TrayMenuItem> getItems() {
        return items;
    }

    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        MenuItem root = new MenuItem(menu, SWT.CASCADE);
        root.setText(getText());
        root.setEnabled(!isDisabled());

        if (getImage() != null) {
            root.setImage(ctx.createImage(Utils.toSWTImage(getImage())));
        }

        textProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!root.isDisposed()) {
                display.asyncExec(() -> root.setText(newValue));
            }
        });

        disableProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!root.isDisposed()) {
                display.asyncExec(() -> root.setEnabled(!newValue));
            }
        });

        imageProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!root.isDisposed()) {
                display.asyncExec(() -> root.setImage(ctx.createImage(Utils.toSWTImage(newValue))));
            }
        });

        Menu subMenu = new Menu(menu);
        root.setMenu(subMenu);

        for (TrayMenuItem item : items) {
            item.create(display, subMenu, ctx);
        }
    }
}
