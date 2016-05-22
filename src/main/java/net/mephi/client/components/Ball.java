package net.mephi.client.components;

import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Базовый класс шаров. Игроки и клетки.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class Ball {
    private Logger log = Logger.getLogger(Ball.class);


    public static final int START_CLIENT_RADIUS = (int) (Board.USERFIELD_HEIGHT * 0.05);
    public static final int FOOD_RADIUS = START_CLIENT_RADIUS / 4;
    public static final int END_GAME_RADIUS = 0;
    public static final double MAX_RADIUS = Board.USERFIELD_HEIGHT / 3;
    public static final String FOOD_NAME = "";
    public static final int LINE_SPACE_SIZE = START_CLIENT_RADIUS;
    private Point centerGlobal = new Point(0, 0); //используется только для елы(клеток)
    private double radius;
    private Point cursorLocation = new Point(0, 0);

    private Rectangle userField = new Rectangle(0, 0, 0, 0);

    private String name = "";
    private boolean visible = true;
    private Color color;
    private boolean isFood = false;
    private Point linesShift = new Point(0, 0);

    public Ball(String name, int radius) {
        this.radius = radius;
        this.name = name;
        setRandomColor();
    }

    /**
     * Положение центра в локальных координатах
     *
     * @return
     */
    public Point getCenterLocalPosition() {
        return new Point(userField.width / 2, userField.height / 2);
    }

    public Point getCenterGlobalPosition() {
        if (isFood) {
            return centerGlobal;
        } else {
            return new Point(userField.x + userField.width / 2, userField.y + userField.height / 2);
        }
    }


    public Point getCenterGlobal() {
        return centerGlobal;
    }

    public static Point getLeftTopPosition(Point center, int radius) {
        return new Point(center.x - radius, center.y - radius);
    }

    public void setCursorLocation(Point p) {
        cursorLocation = p;
    }


    public int getRadius() {
        return (int) Math.round(radius);
        //        return radius;
    }

    public double getRadiusDouble() {
        return radius;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        this.color = c;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public Rectangle getUserField() {
        return userField;
    }

    public void setUserField(Rectangle rect) {
        this.userField = rect;
    }

    public void setUserFieldPosition(Point p) {
        userField.x = p.x;
        userField.y = p.y;
    }

    public void setFood(boolean isFood) {
        this.isFood = isFood;
    }

    public boolean isFood() {
        return isFood;
    }

    /**
     * Увеличить площадь шарика на площадь b
     *
     * @param b
     */
    public void increaseMass(Ball b) {

        double r = ((Math.sqrt(radius * radius + (b.getRadius() * b.getRadius()))));
        if (r > MAX_RADIUS) {
            radius = MAX_RADIUS;
        } else {
            radius = r;
        }


    }



    /**
     * Рассчитать следующюю позицию поля(левый верхний угол) шарика по полученным координатам мыши.
     *
     * @param relativeCursorLocation локальные координаты курсора
     * @return
     */
    public Point countNewFieldPosition(Point relativeCursorLocation) {
        //Магия подобных треугольников по углу и подобным сторонам.
        //ABC ~ A1B1C1 A -общий -> AB/A1B1 = AC/A1C1 = BC/B1C1
        //
        //log.debug("cursor position "+relativeCursorLocation);
        double AB = Math.sqrt(
            Math.pow(relativeCursorLocation.x - getCenterLocalPosition().x, 2) + Math
                .pow(relativeCursorLocation.y - getCenterLocalPosition().y, 2));//всегда >0
        double AC = relativeCursorLocation.x - getCenterLocalPosition().x;//<0 если шаг влево
        double BC = relativeCursorLocation.y - getCenterLocalPosition().y;//<0 если шаг вверх
        double A1B1 = AB / radius * 0.7;

        double A1C1 = A1B1 * AC / AB;//<0 если шаг влево
        double B1C1 = A1B1 * BC / AB;//<0 если шаг вверх



        int x = (int) Math.round(getCenterLocalPosition().x + A1C1);
        int y = (int) Math.round(getCenterLocalPosition().y + B1C1);



        //Если шаг перескакивает курсор
        if (relativeCursorLocation.x > getCenterLocalPosition().x) {
            if (x > relativeCursorLocation.x) {
                return new Point(userField.x, userField.y);
            }
        } else {
            if (x < relativeCursorLocation.x) {
                return new Point(userField.x, userField.y);
            }
        }

        if (relativeCursorLocation.y < getCenterLocalPosition().y) {
            if (y < relativeCursorLocation.y) {
                return new Point(userField.x, userField.y);
            }
        } else {
            if (y > relativeCursorLocation.y) {
                return new Point(userField.x, userField.y);
            }
        }



        int xGlobal = userField.x + (x - getCenterLocalPosition().x);//глобальные
        int yGlobal = userField.y + (y - getCenterLocalPosition().y);

        boolean freezX = false, freezY = false;
        //Проверка выхода за глобальные границы
        if (xGlobal + userField.width / 2 > Board.WIDTH) {//правый
            xGlobal = Board.WIDTH - userField.width / 2;
            freezX = true;
        }
        if (yGlobal + userField.height / 2 > Board.HEIGHT) {//низ
            yGlobal = Board.HEIGHT - userField.height / 2;
            freezY = true;
        }
        if (xGlobal + userField.width / 2 < 0) {//лево
            xGlobal = -userField.width / 2;
            freezX = true;
        }
        if (yGlobal + userField.height / 2 < 0) {//вверх
            yGlobal = -userField.height / 2;
            freezY = true;
        }

        updateLineShift(x, y, freezX, freezY);


        log.debug("lineshift new " + linesShift);
        userField.x = xGlobal;
        userField.y = yGlobal;
        Point p = new Point(x + userField.width / 2, y + userField.height / 2);
        return p;
    }

    /**
     * Проверка пересечения двух шаров
     *
     * @param otherBall
     * @return true, если расстояние между центрами шаров меньше 95% от суммы их радиусов.
     */
    public boolean checkCollisionTo(Ball otherBall) {
        double minCollisionLength = 0.95 * (radius + otherBall.getRadius());
        return (Ball.getCenterDistance(this.getCenterGlobalPosition(),
            otherBall.getCenterGlobalPosition()) < minCollisionLength);
    }

    /**
     * Проверка, может ли данный шар поглотить enemyBall
     *
     * @param enemyBall Другой шар
     * @return true, если данный и enemyBall пересекаются и enemyBall меньше.
     */
    public boolean isThisCanIt(Ball enemyBall) {
        return checkCollisionTo(enemyBall) && enemyBall.getRadius() < this.getRadius();

    }

    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    /**
     * Выставить случайную позицию на поле.
     * Считается на сервере
     * Отступ от границ - 5%
     */
    public void setRandomFieldPosition() {
        userField.x = ThreadLocalRandom.current()
            .nextInt(0, (int) Math.round(userField.width - userField.width * 0.05) + 1);
        userField.y = ThreadLocalRandom.current()
            .nextInt(0, (int) Math.round(userField.height - userField.height * 0.05) + 1);
    }

    public Point setRandomCenterPosition() {
        int x =
            ThreadLocalRandom.current().nextInt(0, (int) (Board.WIDTH - Board.WIDTH * 0.05) + 1);
        int y =
            ThreadLocalRandom.current().nextInt(0, (int) (Board.HEIGHT - Board.HEIGHT * 0.05) + 1);
        centerGlobal = new Point(x, y); //глобальные для еды.
        return centerGlobal;
    }

    public void setRandomColor() {
        Random r = new Random();
        float intensity = r.nextFloat();
        float brightness = r.nextFloat();
        brightness = brightness < 0.5f ? brightness + 0.4f : brightness;
        intensity = intensity < 0.5f ? intensity + 0.4f : intensity;
        color = java.awt.Color.getHSBColor(r.nextFloat(), intensity, brightness);

    }

    /**
     * Расстояние между центрами
     *
     * @param center1
     * @param center2
     * @return
     */
    public static double getCenterDistance(Point center1, Point center2) {
        return Math.sqrt(Math.pow(center2.x - center1.x, 2) + Math.pow(center2.y - center1.y, 2));
    }

    public Point getLeftTopFieldPosition() {
        return new Point(userField.x, userField.y);
    }

    /**
     * Проверка, попадает otherBall в область вилимости данного шара
     *
     * @param otherBall
     * @return
     */
    public boolean isBallInCurrentField(Ball otherBall) {
        Point center;
        if (otherBall.isFood()) {
            center = otherBall.centerGlobal;
        } else {
            center = otherBall.getCenterGlobalPosition();
        }
        return center.x >= userField.x - Ball.MAX_RADIUS
            && center.x <= userField.x + userField.width + Ball.MAX_RADIUS
            && center.y >= userField.y - Ball.MAX_RADIUS
            && center.y <= userField.y + userField.height + Ball.MAX_RADIUS;
    }

    public Point getLinesShift() {
        return linesShift;
    }

    @Override public String toString() {
        return getName() + " " + getRadius() + " " + getCenterLocalPosition();
    }

    /**
     * Поскольку смещается поле, а шарик всегда в центре, то двигаем сетку на фоне.
     * Пересчет смещения сетки.
     *
     * @param x      - новые координаты центра, после шага.
     * @param y      - новые координаты центра, после шага.
     * @param freezX - достигли края (право или лево)
     * @param freezY - достигла края (верх или них)
     * @return смещение сетки.
     */
    public Point updateLineShift(int x, int y, boolean freezX, boolean freezY) {
        log.debug("lineshift old " + linesShift);


        //Смещение сетки на доске 1.
        int tempW = (getCenterLocalPosition().x - x) % Ball.LINE_SPACE_SIZE;
        int tempH = (getCenterLocalPosition().y - y) % Ball.LINE_SPACE_SIZE;

        this.linesShift.x = (freezX ? linesShift.x : linesShift.x + tempW) % Ball.LINE_SPACE_SIZE;
        this.linesShift.y = (freezY ? linesShift.y : linesShift.y + tempH) % Ball.LINE_SPACE_SIZE;

        return new Point(0, 0);
    }


}
