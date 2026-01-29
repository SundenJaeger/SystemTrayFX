package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.*;

import java.util.*;
import java.util.List;

public class SystemTrayFX implements ISystemTrayFX {
    private Display display;
    private Shell shell;
    private Menu menu;
    private TrayItem trayItem;
    private TrayNotification trayNotification;

    private final Queue<Runnable> pendingActions = new ArrayDeque<>();
    private final List<MenuItem> menuItems = new ArrayList<>();
    private final List<org.eclipse.swt.graphics.Image> swtImages = new ArrayList<>();
    private volatile boolean initialized = false;

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

    public void addEntry(MenuItem... items) {
        if (!initialized) {
            menuItems.addAll(List.of(items));
        } else {
            display.asyncExec(() -> {
                for (MenuItem item : items) {
                    item.create(display, menu, this);
                }
            });
        }
    }

    @Override
    public void addMenuItem(javafx.scene.control.MenuItem fxItem) {
//        Runnable task = () -> {
//            MenuItem menuItem = Utils.convertFXItem(display, menu, fxItem);
//        };
//        if (!initialized) {
//            pendingActions.add(task);
//        } else {
//            display.asyncExec(task);
//        }
    }

    @Override
    public void addMenuItem(String text, Image image, Runnable action) {
//        Runnable task = () -> {
//            MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
//            menuItem.setText(text);
//
//            if (image != null) {
//                menuItem.setImage(createImage(Utils.toSWTImage(image)));
//            }
//            menuItem.addListener(SWT.Selection, event -> Platform.runLater(action));
//        };
//
//        if (!initialized) {
//            pendingActions.add(task);
//        } else {
//            display.asyncExec(task);
//        }
    }

    @Override
    public void addExitItem(String text, Image image) {
//        Runnable task = () -> {
//            MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
//            menuItem.setText(text);
//
//            if (image != null) {
//                menuItem.setImage(createImage(Utils.toSWTImage(image)));
//            }
//            menuItem.addListener(SWT.Selection, event -> Platform.runLater(() -> {
//                dispose();
//                Platform.exit();
//            }));
//        };
//
//        if (!initialized) {
//            pendingActions.add(task);
//            return;
//        }
//
//        display.asyncExec(task);
    }

    @Override
    public void addSeparator() {
//        Runnable task = () -> new MenuItem(menu, SWT.SEPARATOR);
//
//        if (!initialized) {
//            pendingActions.add(task);
//            return;
//        }
//
//        display.asyncExec(task);
    }

    @Override
    public void show() {
        Thread swtThread = new Thread(() -> {
            display = new Display();
            shell = new Shell(display);
            menu = new Menu(shell, SWT.POP_UP);

            final Tray tray = display.getSystemTray();
            if (tray == null) {
                display.dispose();
                menu.dispose();
                return;
            }

            trayItem = new TrayItem(tray, SWT.NONE);
            trayItem.setToolTipText(titleProperty.get());
            trayItem.setImage(createImage(Utils.toSWTImage(trayIcon)));

            titleProperty.addListener((observable, oldValue, newValue) -> {
                if (display == null || display.isDisposed()) return;

                display.asyncExec(() -> {
                    if (trayItem != null && !trayItem.isDisposed()) {
                        trayItem.setToolTipText(newValue);
                    }
                });
            });

            trayItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (isMinimizeToTray && !stage.isShowing()) {
                        Platform.runLater(() -> {
                            stage.show();
                            stage.toFront();
                        });
                    }

                    if (stage.isIconified()) {
                        Platform.runLater(() -> {
                            stage.setIconified(false);
                            stage.toFront();
                        });
                    }

                    if (!stage.isFocused()) {
                        Platform.runLater(stage::toFront);
                    }
                }
            });
            trayItem.addListener(SWT.MenuDetect, event -> menu.setVisible(true));

            trayNotification = new TrayNotification(display, shell, trayItem);

            initialized = true;
//            for (Runnable task : pendingActions) {
//                display.asyncExec(task);
//            }
//            pendingActions.clear();

            display.asyncExec(() -> {
                for (MenuItem item : menuItems) {
                    item.create(display, menu, this);
                }
                menuItems.clear();
            });

            while (!display.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        }, "SWT-SystemTrayFX");

        swtThread.setDaemon(true);
        swtThread.start();
    }

    @Override
    public void dispose() {
        if (display == null || display.isDisposed()) return;

        display.asyncExec(() -> {
            if (trayItem != null && !trayItem.isDisposed()) trayItem.dispose();

            if (menu != null && !menu.isDisposed()) menu.dispose();

            for (var image : swtImages) {
                image.dispose();
            }

            if (trayNotification != null) trayNotification.dispose();

            display.dispose();
        });
    }

    @Override
    public void showNotification(String title, String message, NotificationIcon icon, Runnable action) {
        if (!initialized || trayNotification == null) return;
        trayNotification.show(title, message, NOTIFICATION_TIMEOUT, icon, action);
    }

    protected org.eclipse.swt.graphics.Image createImage(ImageData imageData) {
        org.eclipse.swt.graphics.Image image = new org.eclipse.swt.graphics.Image(display, imageData);
        swtImages.add(image);

        return image;
    }
}