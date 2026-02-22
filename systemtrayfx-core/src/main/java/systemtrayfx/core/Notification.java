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

package systemtrayfx.core;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A utility class for displaying system tray notifications.
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * Notification.info("Update Available", "Version 2.0 is ready to install");
 * }</pre>
 *
 * <p>This is a utility class with static methods only and cannot be instantiated.
 *
 * @see TrayNotification
 * @see NotificationType
 */
public final class Notification {

    /* ---------------- Static Fields ---------------- */

    /**
     * The underlying tray notification handler (volatile for thread-safe lazy initialization)
     */
    private static volatile TrayNotification tray;

    /**
     * Queue for notifications created before the tray is initialized
     */
    private static final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();

    /* ---------------- Constructors ---------------- */

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException if called via reflection
     */
    private Notification() {
        throw new UnsupportedOperationException("Notification is a utility class and cannot be instantiated");
    }

    /* ---------------- Public API ---------------- */

    /**
     * Displays an information notification.
     *
     * @param title   the notification title
     * @param message the notification message
     * @throws NullPointerException if title or message is null
     */
    public static void info(String title, String message) {
        info(title, message, null);
    }

    /**
     * Displays an information notification.
     *
     * @param title    the notification title
     * @param message  the notification message
     * @param onAction optional action invoked when the notification is clicked
     * @throws NullPointerException if title or message is null
     */
    public static void info(String title, String message, EventHandler<ActionEvent> onAction) {
        build(title, message, NotificationType.INFORMATION, onAction);
    }

    /**
     * Displays a warning notification.
     *
     * @param title   the notification title
     * @param message the notification message
     * @throws NullPointerException if title or message is null
     */
    public static void warn(String title, String message) {
        warn(title, message, null);
    }

    /**
     * Displays a warning notification.
     *
     * @param title    the notification title
     * @param message  the notification message
     * @param onAction optional action invoked when the notification is clicked
     * @throws NullPointerException if title or message is null
     */
    public static void warn(String title, String message, EventHandler<ActionEvent> onAction) {
        build(title, message, NotificationType.WARNING, onAction);
    }

    /**
     * Displays an error notification.
     *
     * @param title   the notification title
     * @param message the notification message
     * @throws NullPointerException if title or message is null
     */
    public static void error(String title, String message) {
        error(title, message, null);
    }

    /**
     * Displays an error notification.
     *
     * @param title    the notification title
     * @param message  the notification message
     * @param onAction optional action invoked when the notification is clicked
     * @throws NullPointerException if title or message is null
     */
    public static void error(String title, String message, EventHandler<ActionEvent> onAction) {
        build(title, message, NotificationType.ERROR, onAction);
    }

    /**
     * Disposes of all resources and clears pending notifications.
     *
     * <p>This method should be called when shutting down the application to ensure
     * proper cleanup. After calling dispose, the notification system must be
     * re-registered before it can be used again.
     *
     * <p>This method is thread-safe and can be called multiple times safely.
     */
    public static void dispose() {
        if (tray != null) {
            tray.dispose();
            tray = null;
        }
        pending.clear();
    }

    /* ---------------- Helpers ---------------- */

    /**
     * Registers a {@link TrayNotification} instance for handling notifications.
     *
     * <p>This method should be called once during application initialization.
     * After registration, all pending notifications will be displayed automatically.
     *
     * <p>This method is thread-safe and uses a volatile field to ensure visibility
     * across threads.
     *
     * @param notification the TrayNotification instance to register
     * @throws NullPointerException if notification is null
     */
    static void register(TrayNotification notification) {
        Objects.requireNonNull(notification, "TrayNotification is null");
        tray = notification;

        Runnable r;
        while ((r = pending.poll()) != null) {
            r.run();
        }
    }

    /**
     * Internal method to build and show a notification with the specified parameters.
     *
     * <p>If the tray is not yet initialized, the notification is queued for later display.
     *
     * @param title   the notification title
     * @param message the notification message
     * @param type    the notification type
     * @throws NullPointerException if title or message is null
     */
    private static void build(String title, String message, NotificationType type, EventHandler<ActionEvent> onAction) {
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");

        Runnable task = () -> tray.show(title, message, type, onAction);

        if (tray == null) {
            pending.offer(task);
        } else {
            task.run();
        }
    }
}