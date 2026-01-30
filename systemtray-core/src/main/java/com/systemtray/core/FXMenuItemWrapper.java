package com.systemtray.core;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class FXMenuItemWrapper extends TrayMenuItem {
    private final javafx.scene.control.MenuItem fxItem;

    public FXMenuItemWrapper(javafx.scene.control.MenuItem fxItem) {
        this.fxItem = fxItem;
    }

    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        org.eclipse.swt.widgets.MenuItem menuItem;
        final String text = fxItem.getText() != null ? fxItem.getText() : "Item";

        if (fxItem instanceof CheckMenuItem checkMenuItem) {
            menuItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.CHECK);
            menuItem.setText(text);
            menuItem.setSelection(checkMenuItem.isSelected());
            menuItem.setEnabled(!checkMenuItem.isDisable());

            if (checkMenuItem.getOnAction() != null) {
                menuItem.addListener(SWT.Selection, event -> Platform.runLater(() -> checkMenuItem.getOnAction().handle(new ActionEvent())));
            }

            menuItem.addListener(SWT.Selection, event -> {
                if (!menuItem.isDisposed()) {
                    boolean newValue = menuItem.getSelection();

                    if (!checkMenuItem.selectedProperty().isBound()) {
                        Platform.runLater(() -> checkMenuItem.selectedProperty().set(newValue));
                    }
                }
            });

            checkMenuItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (display == null || display.isDisposed()) return;

                if (!menuItem.isDisposed()) {
                    display.asyncExec(() -> menuItem.setSelection(newValue));
                }
            });
        } else if (fxItem instanceof SeparatorMenuItem) {
            menuItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
        } else {
            menuItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
            menuItem.setText(text);
            menuItem.setEnabled(!fxItem.isDisable());

            if (fxItem.getOnAction() != null) {
                menuItem.addListener(SWT.Selection, event -> Platform.runLater(() -> fxItem.getOnAction().handle(new ActionEvent())));
            }
        }

        fxItem.disableProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setEnabled(!newValue));
            }
        });

        fxItem.textProperty().addListener((observable, oldValue, newValue) -> {
            if (display == null || display.isDisposed()) return;

            if (!menuItem.isDisposed()) {
                display.asyncExec(() -> menuItem.setText(newValue));
            }
        });
    }
}
