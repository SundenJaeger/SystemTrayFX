package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class FXMenuItemWrapper extends TrayMenuItem {
    private final javafx.scene.control.MenuItem fxItem;

    public FXMenuItemWrapper(javafx.scene.control.MenuItem fxItem) {
        this.fxItem = fxItem;
    }

    @Override
    protected int getSWTStyle() {
        if (fxItem instanceof CheckMenuItem) return SWT.CHECK;
        if (fxItem instanceof SeparatorMenuItem) return SWT.SEPARATOR;
        if (fxItem instanceof javafx.scene.control.Menu) return SWT.CASCADE;
        return SWT.PUSH;
    }

    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        final String text = fxItem.getText() != null ? fxItem.getText() : getText();

        org.eclipse.swt.widgets.MenuItem menuItem = new MenuItem(menu, getSWTStyle());

        setText(text);
        setDisable(fxItem.isDisable());

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
