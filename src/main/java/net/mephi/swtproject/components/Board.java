package net.mephi.swtproject.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import java.util.concurrent.ThreadLocalRandom;

public class Board extends Canvas {

    private final int WIDTH = 1300;// ширина клетчатого поля
    private final int HEIGHT = 700;// высота клетчатого поля
    private final int DELAY = 100;

    private final int maxFoodAmount = 15;// количество шариков
    private Food[] food = new Food[maxFoodAmount];// массив типа food

    private int foodSize = 10;
    private int currentFoodAmount = maxFoodAmount;

    Ball ball = new Ball();//твой управляемый шарик
    Ball[] enemies = new Ball[5];//количество ботов

    private boolean inGame = true;


    private Display display;
    private Shell shell;
    private Runnable runnable;

    public Board(Shell shell) {
        super(shell, SWT.DOUBLE_BUFFERED);

        this.shell = shell;
// вызывается конструктор, который вызывает метод initBoard()
        initBoard();
    }

    private void initBoard() {

        display = shell.getDisplay();

        addListener(SWT.Paint, event -> doPainting(event));
        //задаем слушателя для события Paint, и когда это событие наступает
        //а оно наступает каждые DELAY миллисекунд
        //вызывается метод doPainting

        Color col = new Color(shell.getDisplay(), 255, 255, 255);
        //задается объект типа Color и в его конструктор передается дисплей и 3 аргумента RGB,
        //которые составляют вместе цвет черный
        setBackground(col);//метод задает задний фон
        col.dispose();//уничтожается


        initGame();
    }


    private void initGame() {


        locateFood();
        locateEnemies();

        runnable = new Runnable() { // создали поток
            @Override
//проверяет, переопределён ли метод. Вызывает ошибку компиляции, если метод не найден в родительском классе
            public void run() {

                if (inGame) { //проверяет значение inGame и вызывает 2 метода
                    checkCollision();
                    checkFood();


                }

                display.timerExec(DELAY, this);
                redraw();//метод перерисовки, он вызывает событие Paint в системе
                // а на событии Paint есть слушатель, который по этому событию вызовет метод drawObjects
            }
        };

        display.timerExec(DELAY, runnable);
    }

    ;

    private void doPainting(Event e) {

        GC gc = e.gc;

        Color col = new Color(shell.getDisplay(), 255, 255, 255);//задается белый фон
        gc.setBackground(col);
        col.dispose();

        //сетка на фоне
        Color colLine = new Color(shell.getDisplay(), 162, 162, 162);
        gc.setLineStyle(SWT.LINE_SOLID);
        gc.setLineWidth(1);
        e.gc.setForeground(colLine);
        for (int i = 0; i < WIDTH; i += 20) {
            gc.drawLine(0, i, WIDTH, i);
            gc.drawLine(i, 0, i, HEIGHT);
        }
        colLine.dispose();


        gc.setAntialias(SWT.ON);

        if (inGame) { //если inGame true, то вызывается метод
            drawObjects(e);
        } else { //если inGame false, то вызывается метод
            gameOver(e);
        }
    }

    private void drawObjects(Event e) {

        GC gc = e.gc;
        Point cursorLocation = Display.getCurrent().getCursorLocation();
        //мы узнаем где находистя курсор
        //getCurrent относительно всего экрана
        if (cursorLocation != null) {
            Control c = Display.getCurrent().getFocusControl();
            if (c != null) {

                //подвинуть шарик к курсору
                Point relativeCursorLocation = c.toControl(cursorLocation);
                ball.moveToCursor(relativeCursorLocation);
                e.gc.setBackground(display.getSystemColor(ball.getColor()));
                e.gc.fillOval(ball.leftTop.x, ball.leftTop.y, ball.radius * 2, ball.radius * 2);

                //нарисовать имя
                Font font = new Font(e.display, "Helvetica", ball.radius / 3, SWT.NORMAL);
                Color col = new Color(e.display, 0, 0, 0);
                gc.setFont(font);
                gc.setForeground(col);
                Point size = gc.textExtent(ball.getName());
                gc.drawText(ball.getName(), ball.getCenterPosition().x - size.x / 2, ball.getCenterPosition().y - size.y / 2);
                font.dispose();
                col.dispose();

                for (Food f : food) {
                    if (f.isVisible()) { // если текущий эл массива видимый, т.е. есть свойство isVisible(true),то
                        e.gc.setBackground(display.getSystemColor(f.getColor()));
                        e.gc.fillOval(f.getLeftTop().x, f.getLeftTop().y, Food.FOOD_SIZE_RADIUS * 2, Food.FOOD_SIZE_RADIUS * 2);
                    }
                }
                for (Ball enemy : enemies) {
                    if (enemy.isVisible()) {
                        enemy.moveToCursor(new Point(ThreadLocalRandom.current().nextInt(0, WIDTH + 1), ThreadLocalRandom.current().nextInt(0, HEIGHT + 1)));
                        //enemy.moveToCursor(ball.getCenterPosition());//враги двигаются ко мне


                        e.gc.setBackground(display.getSystemColor(enemy.getColor()));
                        e.gc.fillOval(enemy.leftTop.x, enemy.leftTop.y, enemy.radius * 2, enemy.radius * 2);
                    }
                }

            }
        }
    }

    private void gameOver(Event e) {

        GC gc = e.gc;

        String msg = "Game Over";
        // рисуем текст белым цветом и шрифтом Helvetica
        Font font = new Font(e.display, "Helvetica", 12, SWT.NORMAL);
        Color whiteCol = new Color(e.display, 20, 20, 20);

        gc.setForeground(whiteCol);
        gc.setFont(font);

        Point size = gc.textExtent(msg);

        gc.drawText(msg, (WIDTH - size.x) / 2, (HEIGHT - size.y) / 2);

        font.dispose();
        whiteCol.dispose();

        display.timerExec(-1, runnable);
    }


    public void checkFood() {
//        if (currentFoodAmount < maxFoodAmount) {
//            for (Food f : food) {
//                if (!f.isVisible()) {
//                    f.setX(ThreadLocalRandom.current().nextInt(0, WIDTH + 1));
//                    f.setY(ThreadLocalRandom.current().nextInt(0, HEIGHT + 1));
//                    f.setColor(ThreadLocalRandom.current().nextInt(0, 16 + 1));
//                    f.setVisible(true);
//                }
//            }
//        }
    }

    public void checkCollision() {

        for (Food f : food) {
            if (f.isVisible()) {

                if (ball.checkCollisionTo(f)) { // если находимся ближе чем минимальное расстояние, то
                    f.setVisible(false);
                    currentFoodAmount--; //уменьшается на 1
                    if (currentFoodAmount == 0) {
                        inGame = false; // присваивается значение false, когда currentFoodAmount==0
                    }
                    ball.descreaseSpeed();// уменьшаем скорость
                    ball.increaseMass();// и увеличиваем массу
                    System.out.println("eat!!!");
                }

                for (Ball enemy : enemies) {
                    if (enemy.isVisible() && enemy.checkCollisionTo(f)) {
                        f.setVisible(false);
                        currentFoodAmount--;
                        if (currentFoodAmount == 0) {
                            inGame = false;
                        }
                        enemy.descreaseSpeed();
                        enemy.increaseMass();
                        System.out.println("enemy eat!!!");
                    }

                    if (enemy.isVisible() && ball.checkCollisionTo(enemy)) {
                        if (enemy.getRadius() > ball.getRadius()) {
                            ball.setRadius(0);

                            inGame = false;
                        } else {
                            ball.descreaseSpeed();
                            ball.increaseMass();
                            enemy.setRadius(0);
                            enemy.setVisible(false);
                        }
                    }
                }

            }
        }


    }

    // метод разбрасывает еду по экранну
    public void locateFood() {

        for (int i = 0; i < maxFoodAmount; i++) {
            Food f = new Food();
            f.setColor(ThreadLocalRandom.current().nextInt(0, 16 + 1));//объекту f присваиваем цвет
            //рандомный от 0 до 16
            f.setPosition(new Point(ThreadLocalRandom.current().nextInt(0, WIDTH + 1), ThreadLocalRandom.current().nextInt(0, HEIGHT + 1)));
            // дальше задается x для этого объекта от 0 до WIDTH, дальше y задается
            f.setVisible(true);//этому объекту присваеивается свойство Visible(true)(видимость объекта)
            food[i] = f;// кладем созданный объект в массив food
        }
    }

    public void locateEnemies() {
        for (int i = 0; i < enemies.length; i++) {
            Ball enemy = new Ball();
            enemy.setName("враг");
            enemy.moveTo(new Point(ThreadLocalRandom.current().nextInt(0, WIDTH + 1), ThreadLocalRandom.current().nextInt(0, HEIGHT + 1)));
            enemies[i] = enemy;
        }
    }

    public void setBallName(String name) {
        ball.setName(name);
    }
}
