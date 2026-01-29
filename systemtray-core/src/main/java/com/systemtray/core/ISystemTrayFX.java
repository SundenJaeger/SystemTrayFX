package com.systemtray.core;

import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

interface ISystemTrayFX extends ISystemTray {
    void addMenuItem(MenuItem menuItem);

    void addMenuItem(String text, Image image, Runnable action);

    @Override
    default void addMenuItem(String text, Runnable action) {
        addMenuItem(text, null, action);
    }

    void addExitItem(String text, Image image);

    @Override
    default void addExitItem(String text) {
        addExitItem(text, null);
    }
}
