package com.systemtray.core;

import javafx.application.Platform;
import javafx.scene.image.Image;

public class TrayExitMenuItem extends TrayMenuItem {
    public TrayExitMenuItem() {
        this("Exit");
        setOnAction(event -> {
            dispose();
            Platform.exit();
        });
    }

    public TrayExitMenuItem(String text) {
        this(text, null);
        setOnAction(event -> {
            dispose();
            Platform.exit();
        });
    }

    public TrayExitMenuItem(String text, Image image) {
        super(text, image);
        setOnAction(event -> {
            dispose();
            Platform.exit();
        });
    }
}
