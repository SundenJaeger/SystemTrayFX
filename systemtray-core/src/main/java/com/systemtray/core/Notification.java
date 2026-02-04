package com.systemtray.core;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Notification {
    private static volatile TrayNotification tray;
    private final static int DEFAULT_TIMEOUT = 3000;
    private static final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();

    private Notification() {
    }

    static void register(TrayNotification notification) {
        Objects.requireNonNull(notification, "TrayNotification is null");
        tray = notification;

        Runnable r;
        while ((r = pending.poll()) != null) {
            r.run();
        }
    }

    public static void info(String title, String message) {
        build(title, message, NotificationIcon.INFORMATION);
    }

    public static void warn(String title, String message) {
        build(title, message, NotificationIcon.WARNING);
    }

    public static void error(String title, String message) {
        build(title, message, NotificationIcon.ERROR);
    }

    public static void dispose() {
        if (tray != null) {
            tray.dispose();
            tray = null;
        }
        pending.clear();
    }

    private static void build(String title, String message, NotificationIcon type) {
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");

        Runnable task = () -> tray.show(title, message, DEFAULT_TIMEOUT, type, null);

        if (tray == null) {
            pending.offer(task);
        } else {
            task.run();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String message;
        private int timeout = DEFAULT_TIMEOUT;
        private NotificationIcon type = NotificationIcon.NONE;
        private Runnable action;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder type(NotificationIcon type) {
            this.type = type;
            return this;
        }

        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

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