package com.systemtray.core;

interface ISystemTray {
    void addEntry(MenuItem... items);

    void show();

    void dispose();

    void showNotification(String title, String message, NotificationIcon icon, Runnable action);

    default void showNotification(String title, String message, NotificationIcon icon) {
        showNotification(title, message, icon, null);
    }
}
