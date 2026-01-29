package com.systemtray.core;

interface ISystemTray {
    void addMenuItem(String text, Runnable action);

    void addExitItem(String text);

    default void addExitItem() {
        addExitItem("Exit");
    }

    void addSeparator();

    void show();

    void dispose();

    void showNotification(String title, String message, NotificationIcon icon, Runnable action);

    default void showNotification(String title, String message, NotificationIcon icon) {
        showNotification(title, message, icon, null);
    }
}
