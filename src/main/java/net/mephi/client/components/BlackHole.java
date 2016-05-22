package net.mephi.client.components;

import org.eclipse.swt.graphics.Point;

/**
 * Черные дыры.
 * Номер дыры связан с номером изображения.
 * Всего 5 дыр.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class BlackHole extends Ball {
    private Point center = new Point(0, 0);
    private int imageNumber = 0;

    public BlackHole(int imageNumber) {
        super("", 0);
        setImageNumber(imageNumber);
    }

    public int getImageNumber() {
        return imageNumber;
    }

    private void setImageNumber(int imageNumber) {
        this.imageNumber = imageNumber;
        if (imageNumber == 1) {
            getUserField().width = 388;
            getUserField().height = 388;
        } else if (imageNumber == 2) {
            getUserField().width = 256;
            getUserField().height = 256;
        } else if (imageNumber == 3) {
            getUserField().width = 179;
            getUserField().height = 179;
        } else if (imageNumber == 4) {
            getUserField().width = 521;
            getUserField().height = 521;
        } else if (imageNumber == 5) {
            getUserField().width = 256;
            getUserField().height = 256;
        }
        setRadius(getUserField().width / 2);
    }



    /**
     * Уменьшить шар при приближении к черной дыре
     *
     * @param b подлетевший шар
     */
    public void decreaseMassOf(Ball b) {
        double r1 = Math.sqrt(0.9999 * b.getRadiusDouble() * b.getRadiusDouble());
        if (r1 < 5.0) {
            b.setRadius(Ball.END_GAME_RADIUS);
            b.setVisible(false);
        } else {
            b.setRadius(r1);
        }
    }

}
