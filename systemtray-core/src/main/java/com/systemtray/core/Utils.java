package com.systemtray.core;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import java.awt.image.BufferedImage;

/**
 * Utility class providing helper methods for system tray integration.
 *
 * <p>This class primarily contains image conversion utilities for converting
 * JavaFX and AWT images into SWT {@link ImageData} for use in system tray
 * components. It also includes small, UI-related helper methods used across
 * the tray framework.
 *
 * <p>This is a utility class with static methods only and cannot be instantiated.
 */
final class Utils {

    /* ---------------- Constructors ---------------- */

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException if called via reflection
     */
    private Utils() {
        throw new UnsupportedOperationException("Utils is a utility class and cannot be instantiated");
    }

    /* ---------------- Methods ---------------- */

    /**
     * Converts a JavaFX {@link Image} to SWT {@link ImageData} format.
     *
     * <p>This method first converts the JavaFX Image to a Swing BufferedImage,
     * then converts it to SWT ImageData. This is necessary because JavaFX and SWT
     * use different image representation formats.
     *
     * @param fxImage the JavaFX image to convert
     * @return the converted SWT ImageData
     * @throws NullPointerException if fxImage is null
     * @see #toSWTImage(BufferedImage)
     */
    static ImageData toSWTImage(Image fxImage) {
        return toSWTImage(SwingFXUtils.fromFXImage(fxImage, null));
    }

    /**
     * Converts a {@link BufferedImage} to SWT {@link ImageData} format.
     *
     * <p>This method performs a pixel-by-pixel conversion, preserving the ARGB
     * (Alpha, Red, Green, Blue) color information. The conversion creates a 32-bit
     * image with full alpha channel support.
     *
     * <p>The conversion process:
     * <ol>
     *   <li>Creates a palette with RGB color masks</li>
     *   <li>Iterates through each pixel in the source image</li>
     *   <li>Extracts ARGB components from each pixel</li>
     *   <li>Maps the RGB values to the palette</li>
     *   <li>Sets both the pixel color and alpha channel in the destination</li>
     * </ol>
     *
     * @param image the BufferedImage to convert
     * @return the converted SWT ImageData with full ARGB support
     * @throws NullPointerException if image is null
     */
    static ImageData toSWTImage(BufferedImage image) {
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

    /**
     * Returns a safe UI text value, falling back to the given default when the
     * provided text is null, empty, or blank.
     *
     * @param defaultText the fallback text to use
     * @param text        the candidate text
     * @return a non-null, non-blank text value
     */
    static String safeText(String defaultText, String text) {
        return (text == null || text.isBlank()) ? defaultText : text.strip();
    }
}