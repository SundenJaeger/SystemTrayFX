package com.systemtray.core;

import javafx.application.Platform;
import javafx.scene.image.Image;

public class ExitMenuItem extends MenuItem {
    public ExitMenuItem() {
        super("Exit");
        setOnAction(event -> {
            dispose();
            Platform.exit();
        });
    }

    public ExitMenuItem(String text) {
        super(text);
        setOnAction(event -> {
            dispose();
            Platform.exit();
        });
    }

    public ExitMenuItem(String text, Image image) {
        super(text, image);
        setOnAction(event -> {
            dispose();
            Platform.exit();
        });
    }
}
