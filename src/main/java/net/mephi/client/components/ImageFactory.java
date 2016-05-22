package net.mephi.client.components;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.InputStream;

/**
 * Фабрика картинок черных дыр.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class ImageFactory {
    private static ImageFactory imageFactory;
    private Image blackHoleImage1;
    private Image blackHoleImage2;
    private Image blackHoleImage3;
    private Image blackHoleImage4;

    public Image getBlackHoleImage5() {
        return blackHoleImage5;
    }

    public Image getBlackHoleImage1() {
        return blackHoleImage1;
    }

    public Image getBlackHoleImage2() {
        return blackHoleImage2;
    }

    public Image getBlackHoleImage3() {
        return blackHoleImage3;
    }

    public Image getBlackHoleImage4() {
        return blackHoleImage4;
    }

    private Image blackHoleImage5;

    private ImageFactory(Display display) {
        loadImages(display);
    }

    public static ImageFactory getInstance(Display display) {
        if (imageFactory == null) {
            ImageFactory factory = new ImageFactory(display);
            imageFactory = factory;
            return factory;
        } else {
            return imageFactory;
        }
    }

    private void loadImages(Display display) {
        blackHoleImage1 = getBlackHole1(display);
        blackHoleImage2 = getBlackHole2(display);
        blackHoleImage3 = getBlackHole3(display);
        blackHoleImage4 = getBlackHole4(display);
        blackHoleImage5 = getBlackHole5(display);
    }

    public Image getBlackHole1(Display display) {
        InputStream location =
            this.getClass().getClassLoader().getResourceAsStream("blackhole1.png");
        return new Image(display, location);
    }

    public Image getBlackHole2(Display display) {
        InputStream location =
            this.getClass().getClassLoader().getResourceAsStream("blackhole2.png");
        return new Image(display, location);
    }

    public Image getBlackHole3(Display display) {
        InputStream location =
            this.getClass().getClassLoader().getResourceAsStream("blackhole3.png");
        return new Image(display, location);
    }

    public Image getBlackHole4(Display display) {
        InputStream location =
            this.getClass().getClassLoader().getResourceAsStream("blackhole4.png");
        return new Image(display, location);
    }

    public Image getBlackHole5(Display display) {
        InputStream location =
            this.getClass().getClassLoader().getResourceAsStream("blackhole5.png");
        return new Image(display, location);
    }
}
