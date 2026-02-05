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

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A facade for displaying system tray notifications with support for various notification types.
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Simple notification
 * Notification.info("Update Available", "Version 2.0 is ready to install");
 *
 * // Custom notification with action
 * Notification.builder()
 *     .title("Task Complete")
 *     .message("Your file has been processed")
 *     .timeout(5000)
 *     .type(NotificationType.INFORMATION)
 *     .action(() -> openFile())
 *     .show();
 * }</pre>
 *
 * <p>This is a utility class with static methods only and cannot be instantiated.
 *
 * @see TrayNotification
 * @see NotificationType
 */
public final class Notification {

    /* ---------------- Constants ---------------- */

    /**
     * Default notification display timeout in milliseconds (3 seconds)
     */
    private final static int DEFAULT_TIMEOUT = 3000;

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
     * Displays an information notification with default timeout.
     *
     * <p>This is a convenience method equivalent to:
     * <pre>{@code
     * Notification.builder()
     *     .title(title)
     *     .message(message)
     *     .type(NotificationType.INFORMATION)
     *     .show();
     * }</pre>
     *
     * @param title the notification title
     * @param message the notification message
     * @throws NullPointerException if title or message is null
     */
    public static void info(String title, String message) {
        build(title, message, NotificationType.INFORMATION);
    }

    /**
     * Displays a warning notification with default timeout.
     *
     * <p>This is a convenience method equivalent to:
     * <pre>{@code
     * Notification.builder()
     *     .title(title)
     *     .message(message)
     *     .type(NotificationType.WARNING)
     *     .show();
     * }</pre>
     *
     * @param title the notification title
     * @param message the notification message
     * @throws NullPointerException if title or message is null
     */
    public static void warn(String title, String message) {
        build(title, message, NotificationType.WARNING);
    }

    /**
     * Displays an error notification with default timeout.
     *
     * <p>This is a convenience method equivalent to:
     * <pre>{@code
     * Notification.builder()
     *     .title(title)
     *     .message(message)
     *     .type(NotificationType.ERROR)
     *     .show();
     * }</pre>
     *
     * @param title the notification title
     * @param message the notification message
     * @throws NullPointerException if title or message is null
     */
    public static void error(String title, String message) {
        build(title, message, NotificationType.ERROR);
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
     * @param title the notification title
     * @param message the notification message
     * @param type the notification type
     * @throws NullPointerException if title or message is null
     */
    private static void build(String title, String message, NotificationType type) {
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");

        Runnable task = () -> tray.show(title, message, DEFAULT_TIMEOUT, type, null);

        if (tray == null) {
            pending.offer(task);
        } else {
            task.run();
        }
    }

    /* ---------------- Builder ---------------- */

    /**
     * Creates a new notification builder for customized notifications.
     *
     * <p>The builder pattern allows for fine-grained control over notification
     * properties including timeout, type, and custom actions.
     *
     * @return a new Builder instance
     * @see Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating customized notifications with fine-grained control.
     *
     * <p>The builder uses method chaining for a fluent API:
     * <pre>{@code
     * Notification.builder()
     *     .title("Download Complete")
     *     .message("Your file is ready")
     *     .timeout(5000)
     *     .type(NotificationType.INFORMATION)
     *     .action(() -> System.out.println("Notification clicked"))
     *     .show();
     * }</pre>
     *
     * <p>Only {@code title} and {@code message} are required. All other properties
     * have sensible defaults.
     */
    public static class Builder {
        private String title;
        private String message;
        private int timeout = DEFAULT_TIMEOUT;
        private NotificationType type = NotificationType.NONE;
        private Runnable action;

        /**
         * Sets the notification title.
         *
         * @param title the title text (required)
         * @return this Builder instance for method chaining
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the notification message.
         *
         * @param message the message text (required)
         * @return this Builder instance for method chaining
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the notification timeout duration.
         *
         * <p>Default is 3000ms (3 seconds).
         *
         * @param timeout the timeout in milliseconds
         * @return this Builder instance for method chaining
         */
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the notification type (affects icon and styling).
         *
         * <p>Default is {@link NotificationType#NONE}.
         *
         * @param type the notification type
         * @return this Builder instance for method chaining
         */
        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets an action to execute when the notification is clicked.
         *
         * <p>The action is executed on the SWT thread. For JavaFX operations,
         * wrap the code in {@code Platform.runLater()}.
         *
         * @param action the action to execute (optional)
         * @return this Builder instance for method chaining
         */
        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

        /**
         * Builds and displays the notification.
         *
         * <p>If the tray is not yet initialized, the notification is queued
         * and will be displayed when the tray becomes available.
         *
         * @throws NullPointerException if title or message is null
         */
        public void show() {
            Objects.requireNonNull(title, "Title cannot be null.");
            Objects.requireNonNull(message, "Message cannot be null.");

            Runnable task = () -> tray.show(title, message, timeout, type, action);

            if (tray == null) {
                pending.offer(task);
            } else {
                task.run();
            }
        }
    }
}