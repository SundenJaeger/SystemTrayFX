package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SystemTrayFX implements ISystemTray {
    private Display display;
    private Shell shell;
    private Menu menu;
    private TrayItem trayItem;
    private TrayNotification trayNotification;

    private final ObservableList<TrayMenuItem> items = FXCollections.observableArrayList();
    private final List<org.eclipse.swt.graphics.Image> swtImages = new ArrayList<>();

    private final Queue<TrayMenuItem[]> pendingItems = new ConcurrentLinkedDeque<>();
    private volatile boolean isInitialized = false;

    private final Stage stage;
    private final Image trayIcon;
    private final boolean isMinimizeToTray;

    private final StringProperty titleProperty = new SimpleStringProperty();

    private static final int NOTIFICATION_TIMEOUT = 3000;

    public SystemTrayFX(Stage stage, String title, Image trayIcon) {
        this(stage, title, trayIcon, false);
    }

    public SystemTrayFX(Stage stage, String title, Image trayIcon, boolean isMinimizeToTray) {
        this.stage = stage;
        this.titleProperty.set(title);
        this.trayIcon = trayIcon;
        this.isMinimizeToTray = isMinimizeToTray;

        initSWT();

        Platform.setImplicitExit(!isMinimizeToTray);
        this.stage.setOnCloseRequest(windowEvent -> {
            if (isMinimizeToTray) {
                windowEvent.consume();
                stage.hide();
            } else {
                dispose();
                Platform.exit();
            }
        });

        titleProperty.addListener((obs, oldValue, newValue) -> {
            if (display != null && !display.isDisposed() && trayItem != null && !trayItem.isDisposed()) {
                display.asyncExec(() -> trayItem.setToolTipText(newValue));
            }
        });

        items.addListener((ListChangeListener<TrayMenuItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    display.asyncExec(() -> change.getAddedSubList().forEach(trayMenuItem -> trayMenuItem.create(display, menu, SystemTrayFX.this)));
                }
                if (change.wasRemoved()) {
                    display.asyncExec(() -> change.getRemoved().forEach(TrayMenuItem::dispose));
                }
            }
        });
    }

    public StringProperty titleProperty() {
        return titleProperty;
    }

    public String getTitle() {
        return titleProperty.get();
    }

    public void setTitle(String title) {
        titleProperty.set(title);
    }

    public ObservableList<TrayMenuItem> getItems() {
        return items;
    }

    @Override
    public void addEntry(TrayMenuItem... items) {
        if (isInitialized) {
            this.items.addAll(items);
        } else {
            pendingItems.offer(items);
        }
    }

    @Override
    public void dispose() {
        if (display == null || display.isDisposed()) return;

        display.asyncExec(() -> {
            if (trayItem != null && !trayItem.isDisposed()) trayItem.dispose();

            if (menu != null && !menu.isDisposed()) menu.dispose();

            swtImages.forEach(org.eclipse.swt.graphics.Image::dispose);

            if (trayNotification != null) trayNotification.dispose();

            display.dispose();
        });
    }

    @Override
    public void showNotification(String title, String message, NotificationIcon icon, Runnable action) {
        if (trayNotification == null) return;
        trayNotification.show(title, message, NOTIFICATION_TIMEOUT, icon, action);
    }

    protected org.eclipse.swt.graphics.Image createImage(ImageData imageData) {
        org.eclipse.swt.graphics.Image image = new org.eclipse.swt.graphics.Image(display, imageData);
        swtImages.add(image);

        return image;
    }

    private void initSWT() {
        Thread swtThread = new Thread(() -> {
            display = new Display();
            shell = new Shell(display);
            menu = new Menu(shell, SWT.POP_UP);

            Tray tray = display.getSystemTray();
            if (tray == null) {
                display.dispose();
                return;
            }

            trayItem = new TrayItem(tray, SWT.NONE);
            trayItem.setToolTipText(titleProperty.get());
            trayItem.setImage(createImage(Utils.toSWTImage(trayIcon)));

            trayItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Platform.runLater(() -> {
                        if (isMinimizeToTray && !stage.isShowing()) stage.show();
                        if (stage.isIconified()) stage.setIconified(false);
                        stage.toFront();
                    });
                }
            });
            trayItem.addListener(SWT.MenuDetect, e -> menu.setVisible(true));

            trayNotification = new TrayNotification(display, shell, trayItem);

            isInitialized = true;
            TrayMenuItem[] pending;
            while ((pending = pendingItems.poll()) != null) {
                this.items.addAll(pending);
            }

            while (!display.isDisposed()) {
                if (!display.readAndDispatch()) display.sleep();
            }
        }, "SWT-SystemTrayFX");

        swtThread.setDaemon(true);
        swtThread.start();
    }
}