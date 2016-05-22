package net.mephi.client.components;

import net.mephi.server.Client;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;

/**
 * Доска для перерисовки поля.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class Board extends Canvas {

    /**
     * Размер глобального поля.
     */
    public static final int WIDTH = 10000;
    public static final int HEIGHT = 10000;

    /**
     * Размер окна у пользователя на экране = 90% от ширины(высоты) монитора в пикселях.
     */
    public static final int USERFIELD_WIDTH =
        (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9);
    public static final int USERFIELD_HEIGHT =
        (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.9);


    public static final int FOOD_SIZE_RADIUS = (int) (WIDTH * 0.001 + 5);
    public static final int MAX_FOOD_AMOUNT = (int) (WIDTH * HEIGHT / FOOD_SIZE_RADIUS / 8000);
    public static final int BLACK_HOLES_AMOUNT = 5;

    private Logger log = Logger.getLogger(Board.class);


    private boolean inGame = true;
    private Point cursorLocation = new Point(USERFIELD_WIDTH / 2, USERFIELD_HEIGHT / 2);
    private Display display;
    private Shell shell;
    private String uuid;
    private JSONObject clients = new JSONObject();
    private JSONArray clientsArray = new JSONArray();
    private JSONArray foodArray = new JSONArray();
    private JSONArray blackHoleArray = new JSONArray();
    private JSONArray clientsTop5 = new JSONArray();
    private Point linesShift = new Point(0, 0);
    private Client client = null;
    /**
     * Число кадров в секунду
     */
    private long fps = 1;
    private ImageFactory imageFactory = null;


    public Board(Shell shell) {
        super(shell, SWT.DOUBLE_BUFFERED);

        this.shell = shell;
        display = shell.getDisplay();
        imageFactory = ImageFactory.getInstance(display);
        addListener(SWT.Paint,
            event -> doPainting(event));//Слушатель события PAINT. На это событие вызывает метод
        Color col = new Color(shell.getDisplay(), 255, 255, 255);//Белый фон
        setBackground(col);
        col.dispose();

    }

    /**
     * Считываем новую позицию курсора для следующего цикла
     */
    private void updateCursorLocation() {
        Point cursorMonitorLocation = Display.getCurrent().getCursorLocation();
        if (Display.getCurrent() != null && Display.getCurrent().getFocusControl() != null) {
            this.cursorLocation =
                Display.getCurrent().getFocusControl().toControl(cursorMonitorLocation);

        }
    }

    public Point getCursorLocation() {
        return this.cursorLocation;
    }

    public void setCursorLocation(Point p) {
        this.cursorLocation = p;
    }

    /**
     * Перерисовать доску.
     * Выполнется в потоке SWT.
     */
    public void refreshBoard(JSONObject allResponceData, Point linesShift, Client client,
        long time4OneCycle) {
        this.clients = allResponceData;//Вся полученная информация от мервера
        this.client = client;
        this.fps = Math.round(1000.0 / (time4OneCycle == 0 ? 1 : time4OneCycle));
        this.linesShift = linesShift;
        this.uuid = client.getUUID();
        clientsArray = (JSONArray) allResponceData.get("clients");
        foodArray = (JSONArray) allResponceData.get("food");
        blackHoleArray = (JSONArray) allResponceData.get("blackhole");
        this.clientsTop5 = (JSONArray) allResponceData.get("top5");
        if (clientsArray.size() > 0) {
            Display.getDefault().syncExec(() -> {
                if (!shell.isDisposed()) {
                    redraw();//Вызываем SWT событие Перерисовка (PAINT)
                }
            });
        }


    }

    /**
     * При вызыве события PAINT, слушатель вызывает этот метод.
     *
     * @param e
     */
    private void doPainting(Event e) {
        updateCursorLocation();
        log.debug("doPainting");


        GC gc = e.gc; //SWT объект для рисования
        gc.setAntialias(SWT.ON);//сглаживание краев
        Color col = getWhiteColor(e.display);
        gc.setBackground(col);
        col.dispose();

        //сетка на фоне
        Color colLine = new Color(shell.getDisplay(), 162, 162, 162);
        gc.setLineStyle(SWT.LINE_SOLID);
        gc.setLineWidth(1);
        e.gc.setForeground(colLine);
        for (int i = 0; i < USERFIELD_WIDTH; i += Ball.LINE_SPACE_SIZE) {
            gc.drawLine(0, i + linesShift.y, USERFIELD_WIDTH, i + linesShift.y);//горизонтальные
            gc.drawLine(i + linesShift.x, 0, i + linesShift.x, USERFIELD_HEIGHT);//вертикальные
        }
        colLine.dispose();//для освобождения ресурсов

        log.debug("Number of clients = " + clientsArray.size());
        if (clientsArray.size() > 0) {
            drawTopScores(e);

            for (int i = 0; i < clientsArray.size(); ++i) {
                JSONObject ball = (JSONObject) clientsArray.get(i);
                if (ball.get("uuid").equals(uuid)
                    && ((Long) ball.get("radius")).intValue() == Ball.END_GAME_RADIUS) {
                    inGame = false;
                    log.debug("inGame = false");
                    break;
                }
            }

            //нарисовать координаты курсора
            Font font = new Font(e.display, "Helvetica", 10, SWT.NORMAL);
            Color col1 = new Color(e.display, 0, 0, 0);
            gc.setFont(font);
            gc.setForeground(col1);
            gc.drawText(getCursorLocation().toString(), 20, 20);
            gc.drawText("FPS: " + String.valueOf(fps), 20, 50);
            font.dispose();
            col1.dispose();

            if (inGame) {
                drawObjects(e);
            } else {
                client.endGame();
                gameOver(e);
            }
        }
    }

    /**
     * Нарисовать все объекты (еда, шарики и дыры)
     *
     * @param e
     */
    private void drawObjects(Event e) {
        GC gc = e.gc;

        Control c =
            Display.getCurrent().getFocusControl();//Если поле не в фокусе, то объекты не рисуются
        if (c != null) {
            for (int i = 0; i < clientsArray.size(); ++i) {//Рисуем Клиентов
                JSONObject ball = (JSONObject) clientsArray.get(i);
                Point center =
                    new Point(((Long) ball.get("x")).intValue(), ((Long) ball.get("y")).intValue());
                int radius = ((Long) ball.get("radius")).intValue();
                Color ballColor = getSWTColorFromJSON((JSONObject) ball.get("color"), display);
                Color boardCol = getBlackColor(display);
                e.gc.setBackground(boardCol);
                Point leftTop = Ball.getLeftTopPosition(center, radius);
                e.gc.fillOval(leftTop.x, leftTop.y, radius * 2,
                    radius * 2);//Нарисовать черный шар сзади, чтобы был как тень
                e.gc.setBackground(ballColor);
                e.gc.fillOval(leftTop.x, leftTop.y, radius * 2 - (int) (radius * 2 * 0.02),
                    radius * 2 - (int) (radius * 2 * 0.02));

                //нарисовать имя
                Font font = new Font(e.display, "Helvetica", radius / 3, SWT.NORMAL);
                Color col = new Color(e.display, 0, 0, 0);
                gc.setFont(font);
                gc.setForeground(col);
                Point size = gc.textExtent((String) ball.get("name"));
                gc.drawText((String) ball.get("name"), center.x - size.x / 2,
                    center.y - size.y / 2);
                font.dispose();
                col.dispose();

            }

            //Рисуем клетки еды
            for (int i = 0; i < foodArray.size(); i++) {
                JSONObject food = (JSONObject) foodArray.get(i);
                e.gc.setBackground(getSWTColorFromJSON((JSONObject) food.get("color"), display));
                Point center =
                    new Point(((Long) food.get("x")).intValue(), ((Long) food.get("y")).intValue());
                Point leftTop = Ball.getLeftTopPosition(center, Ball.FOOD_RADIUS);
                e.gc.fillOval(leftTop.x, leftTop.y, Ball.FOOD_RADIUS * 2, Ball.FOOD_RADIUS * 2);
            }

            //Рисуем черные дыры
            for (int i = 0; i < blackHoleArray.size(); i++) {
                JSONObject hole = (JSONObject) blackHoleArray.get(i);
                Point leftTop =
                    new Point(((Long) hole.get("x")).intValue(), ((Long) hole.get("y")).intValue());
                int imageNum =
                    ((Long) hole.get("id")).intValue();//Определяем номер картинки по переданному ID
                BlackHole bh = new BlackHole(imageNum);
                if (imageNum == 1) {
                    gc.drawImage(imageFactory.getBlackHoleImage1(),
                        leftTop.x - bh.getUserField().width / 2,
                        leftTop.y - bh.getUserField().height / 2);
                } else if (imageNum == 2) {
                    gc.drawImage(imageFactory.getBlackHoleImage2(),
                        leftTop.x - bh.getUserField().width / 2,
                        leftTop.y - bh.getUserField().height / 2);
                } else if (imageNum == 3) {
                    gc.drawImage(imageFactory.getBlackHoleImage3(),
                        leftTop.x - bh.getUserField().width / 2,
                        leftTop.y - bh.getUserField().height / 2);
                } else if (imageNum == 4) {
                    gc.drawImage(imageFactory.getBlackHoleImage4(),
                        leftTop.x - bh.getUserField().width / 2,
                        leftTop.y - bh.getUserField().height / 2);
                } else if (imageNum == 5) {
                    gc.drawImage(imageFactory.getBlackHoleImage5(),
                        leftTop.x - bh.getUserField().width / 2,
                        leftTop.y - bh.getUserField().height / 2);
                }
            }

        }
    }

    /**
     * Вызывается, когда радиус у шара = 0.
     * Показываем надпись Game Over.
     *
     * @param e
     */
    private void gameOver(Event e) {

        GC gc = e.gc;

        String msg = "Game Over";

        Font font = new Font(e.display, "Helvetica", 12, SWT.NORMAL);
        Color whiteCol = new Color(e.display, 20, 20, 20);

        gc.setForeground(whiteCol);
        gc.setFont(font);

        Point size = gc.textExtent(msg);

        gc.drawText(msg, (USERFIELD_WIDTH - size.x) / 2, (USERFIELD_HEIGHT - size.y) / 2);

        font.dispose();
        whiteCol.dispose();

    }


    /**
     * Нарисовать ТОП 5
     *
     * @param e
     */
    private void drawTopScores(Event e) {
        GC gc = e.gc;

        Font font = new Font(e.display, "Helvetica", 13, SWT.BOLD);//шрифт
        Color col1 = new Color(e.display, 0, 0, 0);
        gc.setFont(font);
        gc.setForeground(col1);
        gc.drawText("TOP 5:", USERFIELD_WIDTH - 130, 20);


        for (int i = 0; i < clientsTop5.size(); i++) {
            JSONObject ball = (JSONObject) clientsTop5.get(i);
            String name = (String) ball.get("name");
            int score = (((Long) ball.get("score")).intValue() - Ball.START_CLIENT_RADIUS);
            if (score < 0) {
                score = 0;
            }

            String out = i + 1 + ". " + StringUtils.rightPad(name, 5, '.') + "..." + StringUtils
                .leftPad(score + "", 3, '.');//выравнивание по точкам
            Point size = gc.textExtent(out);
            gc.drawText(out, USERFIELD_WIDTH - 50 - size.x, 50 + (size.y * i));
        }
        font.dispose();
        col1.dispose();
    }

    public org.eclipse.swt.graphics.Color getSWTColor(java.awt.Color color, Display d) {
        return new Color(d, color.getRed(), color.getGreen(), color.getBlue());
    }

    public Color getSWTColorFromJSON(JSONObject color, Display d) {
        java.awt.Color c = new java.awt.Color(((Long) color.get("red")).intValue(),
            ((Long) color.get("green")).intValue(), ((Long) color.get("blue")).intValue());
        return getSWTColor(c, d);
    }

    public Color getBlackColor(Display d) {
        return new Color(d, 0, 0, 0);
    }

    public Color getWhiteColor(Display d) {
        return new Color(d, 255, 255, 255);
    }
}
