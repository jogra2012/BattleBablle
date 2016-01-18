package net.mephi.swtproject.components;

import org.eclipse.swt.graphics.Point;

import java.util.concurrent.ThreadLocalRandom;


public class Ball {
    Point center;
    Point leftTop;
    int radius;
    int speed;

    String name = "";
    boolean visible = true;
    int color;

    Ball() {
        center = new Point(0, 0);//храним координаты центра
        leftTop = new Point(center.x - radius, center.y - radius);//координаты верхнего левого угла
        radius = 25;
        speed = 25;
        color = ThreadLocalRandom.current().nextInt(1, 16 + 1);
        this.name = "Anonym";

    }

    public Point getCenterPosition() {
        return center;
    }

    public Point getLeftTopPosition() {
        return leftTop;
    }

    public int getRadius() {
        return radius;
    }

    public int getColor() {
        return color;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }


    public void moveTo(Point newPosition) {
        center = newPosition;
        leftTop.x = center.x - radius;
        leftTop.y = center.y - radius;
    }

    public void descreaseSpeed() {
        if (speed > 1) {
            speed--;
        }
    }

    public void increaseMass() {
        radius += 5;
    }

    public int getSpeed() {
        return speed;
    }

    public void moveToCursor(Point relativeCursorLocation) {
        double L2 = Math.sqrt(Math.pow(relativeCursorLocation.x - center.x, 2) + Math.pow(relativeCursorLocation.y - center.y, 2)) - speed;
        //корень из (( - )^2 + ( - )^2)
        double L1 = getSpeed();
        center.x = (int) ((relativeCursorLocation.x + ((L2 / L1) * center.x)) / (1 + L2 / L1));
        //формула вычисления смещения шарика относительно курсор
        center.y = (int) ((relativeCursorLocation.y + ((L2 / L1) * center.y)) / (1 + L2 / L1));
        leftTop = new Point(center.x - radius, center.y - radius);
    }

    //проверка на столкновение
    public boolean checkCollisionTo(Food food) {
        double minCollisionLength = Food.FOOD_SIZE_RADIUS / 2 + radius;//нашли минимальное расстояние
        double curLen = Math.sqrt(Math.pow(food.getCenter().x - center.x, 2) + Math.pow(food.getCenter().y - center.y, 2));//текущая длина
        //корень из ((значение x этой точки еды - x шарика)^2 + (y еды - y шарика)^2)
        //текущее расстояние от шарика до еды
        return curLen < minCollisionLength;//сравнили
    }

    public boolean checkCollisionTo(Ball enemyBall) {
        double minCollisionLength = radius / 2 + radius;
        double curLen = Math.sqrt(Math.pow(enemyBall.getCenterPosition().x - center.x, 2) + Math.pow(enemyBall.getCenterPosition().y - center.y, 2));
        return curLen < minCollisionLength;
    }

    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

}
