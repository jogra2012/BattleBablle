package net.mephi.swtproject.components;

import org.eclipse.swt.graphics.Point;

public class Food {
    public static final int FOOD_SIZE_RADIUS = 10;// размер еды

    Point center;
    Point leftTop;
    private int color;
    private boolean visible;


    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }


    public void setPosition(Point newPosition) {
        center = newPosition;
        leftTop = new Point(center.x - FOOD_SIZE_RADIUS, center.y - FOOD_SIZE_RADIUS);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Point getCenter() {
        return center;
    }

    public Point getLeftTop() {
        return leftTop;
    }


}
