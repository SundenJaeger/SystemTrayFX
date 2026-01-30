package com.systemtray.core;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import java.awt.image.BufferedImage;

final class Utils {
    public static ImageData toSWTImage(Image fxImage) {
        return toSWTImage(SwingFXUtils.fromFXImage(fxImage, null));
    }

    public static ImageData toSWTImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
        ImageData imageData = new ImageData(width, height, 32, palette);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;

                int pixel = palette.getPixel(new RGB(red, green, blue));
                imageData.setPixel(x, y, pixel);
                imageData.setAlpha(x, y, alpha);
            }
        }

        return imageData;
    }
}