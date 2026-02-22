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
import javafx.beans.property.*;
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

/**
 * A JavaFX-based system tray implementation that integrates with the native system tray
 * using SWT (Standard Widget Toolkit).
 *
 * <p>The system tray runs on a separate SWT thread to prevent blocking the JavaFX
 * application thread.
 *
 * <p><strong>Basic Usage Example:</strong>
 * <pre>{@code
 * // Create system tray
 * Image trayIcon = new Image(getClass().getResourceAsStream("/icon.png"));
 * SystemTrayFX tray = new SystemTrayFX(stage, "My Application", trayIcon);
 *
 * // Add menu items using getItems()
 * tray.getItems().add(new TrayMenuItem("Open", e -> stage.show()));
 * tray.getItems().add(new TrayMenuItem("Settings", e -> openSettings()));
 * tray.getItems().addAll(
 *     new TraySeparatorMenuItem(),
 *     new TrayExitMenuItem("Exit")
 * );
 *
 * // Or add menu items using addEntry()
 * tray.addEntry(new TrayMenuItem("Open", e -> stage.show()));
 * tray.addEntry(new TrayMenuItem("Settings", e -> openSettings()));
 * tray.addEntry(
 *     new TraySeparatorMenuItem(),
 *     new TrayExitMenuItem("Exit")
 * );
 * }</pre>
 *
 * <p><strong>Minimize to Tray Example:</strong>
 * <pre>{@code
 * // Create system tray with minimize-to-tray enabled
 * SystemTrayFX tray = new SystemTrayFX(stage, "My Application", trayIcon, true);
 *
 * // Add menu items (exit item is essential for proper cleanup)
 * tray.getItems().addAll(
 *     new TrayMenuItem("Show", e -> {
 *         stage.show();
 *         stage.toFront();
 *     }),
 *     new TraySeparatorMenuItem(),
 *     new TrayExitMenuItem("Exit")
 * );
 *
 * // Alternative using addEntry()
 * tray.addEntry(
 *     new TrayMenuItem("Show", e -> {
 *         stage.show();
 *         stage.toFront();
 *     }),
 *     new TraySeparatorMenuItem(),
 *     new TrayExitMenuItem("Exit")
 * );
 * }</pre>
 * <p><strong>Menu Items with Icons:</strong>
 * <pre>{@code
 * Image openIcon = new Image(getClass().getResourceAsStream("/open.png"));
 * Image settingsIcon = new Image(getClass().getResourceAsStream("/settings.png"));
 *
 * tray.addEntry(
 *     new TrayMenuItem("Open", openIcon, e -> stage.show()),
 *     new TrayMenuItem("Settings", settingsIcon, e -> openSettings()),
 *     new TrayExitMenuItem("Exit")
 * );
 * }</pre>
 *
 * @see ISystemTray
 * @see TrayMenuItem
 * @see TrayExitMenuItem
 * @see Notification
 */
public class SystemTrayFX implements ISystemTray {

    /* ---------------- SWT Components ---------------- */

    private Display display;
    private Shell shell;
    private Menu menu;
    private TrayItem trayItem;
    private TrayNotification trayNotification;

    /* ---------------- Collections ---------------- */

    /**
     * Observable list of menu items displayed in the tray context menu
     */
    private final ObservableList<TrayMenuItem> items = FXCollections.observableArrayList();

    /**
     * List of SWT images that need to be disposed when the tray is closed
     */
    private final List<org.eclipse.swt.graphics.Image> swtImages = new ArrayList<>();

    /**
     * Queue for menu items added before the SWT thread is initialized
     */
    private final Queue<TrayMenuItem[]> pendingItems = new ConcurrentLinkedDeque<>();

    /* ---------------- State Flags ---------------- */

    /**
     * Flag indicating whether the SWT thread has been initialized
     */
    private volatile boolean isInitialized = false;

    /* ---------------- JavaFX Components ---------------- */

    /**
     * The JavaFX stage associated with this system tray
     */
    private final Stage stage;

    /**
     * Whether the application should minimize to tray instead of closing
     */
    private final BooleanProperty minimizeToTray = new SimpleBooleanProperty(false);

    /**
     * Observable property for the tray icon tooltip text
     */
    private final StringProperty title = new SimpleStringProperty();

    /**
     * Observable property representing the tray icon image
     */
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();

    /* ---------------- Constructors ---------------- */

    /**
     * Creates a system tray icon with the specified configuration.
     * The application will close normally when the window is closed.
     *
     * @param stage    the JavaFX stage to associate with this system tray
     * @param title    the tooltip text displayed when hovering over the tray icon
     * @param trayIcon the icon image to display in the system tray
     */
    public SystemTrayFX(Stage stage, String title, Image trayIcon) {
        this(stage, title, trayIcon, false);
    }

    /**
     * Creates a system tray icon with the specified configuration.
     *
     * <p><strong>Important:</strong> When using {@code minimizeToTray = true}, the application
     * will not exit automatically when the window is closed. To ensure proper cleanup of system
     * tray resources, you must either:
     * <ul>
     *   <li>Add a {@link TrayExitMenuItem} to the tray menu (recommended)</li>
     *   <li>Call {@link #dispose()} manually in a shutdown hook or exit handler</li>
     * </ul>
     *
     * <p><strong>Example with TrayExitMenuItem using getItems():</strong>
     * <pre>{@code
     * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, true);
     * tray.getItems().add(new TrayExitMenuItem("Exit"));
     * }</pre>
     *
     * <p><strong>Example with TrayExitMenuItem using addEntry():</strong>
     * <pre>{@code
     * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, true);
     * tray.addEntry(new TrayExitMenuItem("Exit"));
     * }</pre>
     *
     * <p><strong>Example with shutdown hook:</strong>
     * <pre>{@code
     * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, true);
     * Runtime.getRuntime().addShutdownHook(new Thread(() -> {
     *     tray.dispose();
     * }));
     * }</pre>
     *
     * <p>If {@code minimizeToTray = false}, cleanup is handled automatically when
     * the window closes.
     *
     * @param stage          the JavaFX stage to associate with this system tray
     * @param title          the tooltip text displayed when hovering over the tray icon
     * @param trayIcon       the icon image to display in the system tray
     * @param minimizeToTray if true, closing the window minimizes to tray instead of exiting;
     *                       requires manual cleanup via TrayExitMenuItem or dispose()
     */
    public SystemTrayFX(Stage stage, String title, Image trayIcon, boolean minimizeToTray) {
        this.stage = stage;
        this.title.set(title);
        this.image.set(trayIcon);
        this.minimizeToTray.set(minimizeToTray);

        initSWT();

        stageCloseRequest(minimizeToTray);

        this.minimizeToTray.addListener((observable, oldValue, newValue) -> stageCloseRequest(newValue));

        this.title.addListener((obs, oldValue, newValue) -> {
            if (display != null && !display.isDisposed() && trayItem != null && !trayItem.isDisposed()) {
                display.asyncExec(() -> trayItem.setToolTipText(newValue));
            }
        });

        image.addListener((observable, oldValue, newValue) -> {
            if (display != null && !display.isDisposed() && trayItem != null && !trayItem.isDisposed()) {
                display.asyncExec(() -> trayItem.setImage(createImage(Utils.toSWTImage(newValue))));
            }
        });

        items.addListener((ListChangeListener<TrayMenuItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    List<TrayMenuItem> added = List.copyOf(change.getAddedSubList());

                    if (!isInitialized) {
                        pendingItems.offer(added.toArray(new TrayMenuItem[0]));
                    } else {
                        display.asyncExec(() -> added.forEach(item -> item.create(display, menu, SystemTrayFX.this)));
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

    /* ---------------- Properties ---------------- */

    /**
     * Returns the title property for the tray icon tooltip.
     *
     * @return the StringProperty representing the tooltip text
     */
    public StringProperty titleProperty() {
        return title;
    }

    /**
     * Returns the image property for tray icon.
     *
     * @return the ObjectProperty representing the tray icon image.
     */
    public ObjectProperty<Image> imageProperty() {
        return image;
    }

    /**
     * Returns the property indicating whether the application should minimize to tray.
     *
     * @return BooleanProperty representing the minimize-to-tray setting
     */
    public BooleanProperty minimizeToTrayProperty() {
        return minimizeToTray;
    }

    /**
     * Gets the current tooltip text of the tray icon.
     *
     * @return the tooltip text
     */
    public String getTitle() {
        return title.get();
    }

    /**
     * Sets the tooltip text of the tray icon.
     *
     * @param title the new tooltip text
     */
    public void setTitle(String title) {
        this.title.set(title);
    }

    /**
     * Gets the current tray icon image.
     *
     * @return the tray icon image
     */
    public Image getImage() {
        return image.get();
    }

    /**
     * Sets the tray icon image.
     * <p>
     * The provided image must not be {@code null}.
     *
     * @param trayIcon the new tray icon image
     * @throws NullPointerException if {@code trayIcon} is {@code null}
     */
    public void setImage(Image trayIcon) {
        Objects.requireNonNull(trayIcon, "Tray icon cannot be null");
        image.set(trayIcon);
    }

    /**
     * Gets the current minimize-to-tray setting.
     *
     * @return {@code true} if the application is set to minimize to tray, {@code false} otherwise
     */
    public boolean isMinimizeToTray() {
        return minimizeToTray.get();
    }

    /**
     * Sets whether the application should minimize to the tray.
     *
     * @param minimizeToTray the new minimize-to-tray setting
     */
    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray.set(minimizeToTray);
    }

    /**
     * Returns the observable list of menu items.
     * Items can be added or removed from this list to update the tray menu.
     *
     * @return the observable list of TrayMenuItem objects
     */
    public ObservableList<TrayMenuItem> getItems() {
        return items;
    }

    /* ---------------- Public API ---------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEntry(TrayMenuItem... items) {
        if (isInitialized) {
            this.items.addAll(items);
        } else {
            pendingItems.offer(items);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        if (display == null || display.isDisposed()) return;

        display.asyncExec(() -> {
            if (trayItem != null && !trayItem.isDisposed()) trayItem.dispose();

            if (menu != null && !menu.isDisposed()) menu.dispose();

            swtImages.forEach(org.eclipse.swt.graphics.Image::dispose);

            if (trayNotification != null) trayNotification.dispose();

            display.dispose();

            Notification.dispose();
        });
    }

    /* ---------------- Protected Methods ---------------- */

    /**
     * Creates an SWT image from the provided image data.
     * <p>
     * The image is scaled based on the system DPI to ensure the tray icon
     * renders at an appropriate size on high-DPI displays.
     * </p>
     * <p>
     * The created image is tracked for proper disposal.
     * </p>
     *
     * @param imageData the image data to convert
     * @return the created, DPI-scaled SWT image
     */
    protected org.eclipse.swt.graphics.Image createImage(ImageData imageData) {
        int dpiX = display.getDPI().x;
        int dpiY = display.getDPI().y;

        //96 for baseline DPI (1920 x 1080 | Scaling: 100%)
        float scaleX = dpiX / 96f;
        float scaleY = dpiY / 96f;

        //Windows tray icon is 16 x 16, so I think this should be good?
        int width = Math.round(16 * scaleX);
        int height = Math.round(16 * scaleY);

        org.eclipse.swt.graphics.Image image = new org.eclipse.swt.graphics.Image(display, imageData.scaledTo(width, height));
        swtImages.add(image);

        return image;
    }

    /* ---------------- Helpers ---------------- */

    /**
     * Initializes the SWT components on a separate daemon thread.
     * Creates the display, shell, menu, tray item, and sets up event handlers.
     */
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
            trayItem.setToolTipText(title.get());
            trayItem.setImage(createImage(Utils.toSWTImage(image.get())));

            trayItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Platform.runLater(() -> {
                        if (isMinimizeToTray() && !stage.isShowing()) stage.show();
                        if (stage.isIconified()) stage.setIconified(false);
                        stage.toFront();
                    });
                }
            });
            trayItem.addListener(SWT.MenuDetect, e -> menu.setVisible(true));

            trayNotification = new TrayNotification(display, shell, trayItem);
            Notification.register(trayNotification);

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

    private void stageCloseRequest(boolean minimizeToTray) {
        Platform.setImplicitExit(!minimizeToTray);

        stage.setOnCloseRequest(event -> {
            if (minimizeToTray) {
                event.consume();
                stage.hide();
            } else {
                dispose();
                Platform.exit();
            }
        });
    }
}