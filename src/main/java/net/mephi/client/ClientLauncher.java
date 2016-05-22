package net.mephi.client;

import net.mephi.client.components.Ball;
import net.mephi.client.components.Board;
import net.mephi.client.components.InputDialog;
import net.mephi.server.Client;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * Стартовый класс клиента.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class ClientLauncher {

    private Logger log = Logger.getLogger(ClientLauncher.class);

    public static void main(String[] args) {
        ClientLauncher main = new ClientLauncher();
        if (args.length == 2) {
            main.start(args[0], args[1]);
        } else {
            main.start("localhost", "6999");
        }
    }


    /**
     * Запуск работы клиента.
     *
     * @param host ip-адрес для подключения к серверу
     * @param port порт для подключения к серверу.
     */
    private void start(String host, String port) {
        Display display = new Display();
        Shell shell = new Shell(display, SWT.SHELL_TRIM | SWT.CENTER);
        shell.setText("Bubble battle");
        shell.setSize(Board.USERFIELD_WIDTH, Board.USERFIELD_HEIGHT);

        Monitor primary = display.getPrimaryMonitor();
        Rectangle bounds = primary.getBounds();
        Rectangle rect = shell.getBounds();

        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;

        shell.setLocation(x, y);
        createTopMenu(shell);//Создание верхнего меню (Файл, помощь)
        FillLayout layout = new FillLayout();
        shell.setLayout(layout);



        Board board = new Board(shell);
        shell.open();
        InputDialog dlg = new InputDialog(shell);

        String inputName = dlg.open();
        if (inputName == null) {
            inputName = "";
        } else {
            inputName = inputName.substring(0, Math.min(5, inputName.length()));
        }
        Client client = new Client();
        client.setClient(client);
        Ball ball = new Ball(inputName, Ball.START_CLIENT_RADIUS);
        log.debug("Ball max radius " + Ball.MAX_RADIUS);
        log.debug("Food radius = " + Ball.FOOD_RADIUS);
        log.debug("start ball radius = " + Ball.START_CLIENT_RADIUS);
        log.debug("line space = " + Ball.LINE_SPACE_SIZE);
        client.setBall(ball);
        client.setBoard(board);

        //Слушатель нажатия на крестик окна
        shell.addListener(SWT.Close, event -> {
            log.debug("End game  " + client);
            client.endGame();
            display.dispose();
        });

        //Отправка данных для регистрации на сервере
        connectToServer(host, port, client);
        client.startGame();
        while (!shell.isDisposed()) {//
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        client.endGame();
        display.dispose();
    }

    /**
     * Отправка данных для регистрации на сервере.
     *
     * @param host
     * @param port
     * @param client
     */
    private void connectToServer(String host, String port, Client client) {
        try {

            Socket clientSocket = new Socket(host, Integer.parseInt(port));

            //Отправляем имя шарика, цвет, размер экрана.
            JSONObject obj = new JSONObject();
            obj.put("type", "register");
            obj.put("name", client.getBall().getName());
            JSONObject color = new JSONObject();
            color.put("red", client.getBall().getColor().getRed());
            color.put("green", client.getBall().getColor().getGreen());
            color.put("blue", client.getBall().getColor().getBlue());
            obj.put("color", color);
            obj.put("width", Board.USERFIELD_WIDTH);
            obj.put("height", Board.USERFIELD_HEIGHT);

            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            client.setOis(inFromServer);
            client.setOos(outToServer);
            client.setSocket(clientSocket);
            outToServer.write(obj.toString().getBytes());
            outToServer.flush();
            log.debug("sent: " + obj);

            //Получаем уникальный ID и глобальные координаты окна
            String res = inFromServer.readLine();//Ждет ответ от серевера
            JSONParser parser = new JSONParser();
            try {
                JSONObject o = (JSONObject) parser.parse(res);
                if (o.get("type").equals("register_complete")) {
                    client.setUUID((String) o.get("uuid"));
                    int x = ((Long) o.get("field.x")).intValue();
                    int y = ((Long) o.get("field.y")).intValue();
                    client.getBall().setUserField(
                        new Rectangle(x, y, Board.USERFIELD_WIDTH, Board.USERFIELD_HEIGHT));
                    log.debug("Connected to server; ID=" + client.getUUID());
                }
            } catch (ParseException e) {
                log.error(e);
            }


        } catch (IOException e) {
            log.error(e);

        }

    }


    public void createTopMenu(Shell shell) {
        // Создание системы меню
        Menu main = createMenu(shell, SWT.BAR | SWT.LEFT_TO_RIGHT);
        shell.setMenuBar(main);

        MenuItem fileMenuItem =
            createMenuItem(main, SWT.CASCADE, "&Файл", null, -1, true, null, shell);
        Menu fileMenu = createMenu(shell, SWT.DROP_DOWN, fileMenuItem, true);
        MenuItem exitMenuItem =
            createMenuItem(fileMenu, SWT.PUSH, "&Выход\tCtrl+X", null, SWT.CTRL + 'X', true,
                "doExit", shell);

        MenuItem helpMenuItem =
            createMenuItem(main, SWT.CASCADE, "&Помощь", null, -1, true, null, shell);
        Menu helpMenu = createMenu(shell, SWT.DROP_DOWN, helpMenuItem, true);
        MenuItem aboutMenuItem =
            createMenuItem(helpMenu, SWT.PUSH, "&Об игре\tCtrl+A", null, SWT.CTRL + 'A', true,
                "doAbout", shell);
    }

    protected Menu createMenu(Shell parent, int style) {
        Menu m = new Menu(parent, style);
        return m;
    }

    protected Menu createMenu(Shell parent, int style, MenuItem container, boolean enabled) {
        Menu m = createMenu(parent, style);
        m.setEnabled(enabled);
        container.setMenu(m);
        return m;
    }

    protected MenuItem createMenuItem(Menu parent, int style, String text, Image icon, int accel,
        boolean enabled, String callback, Shell shell) {
        MenuItem mi = new MenuItem(parent, style);
        if (text != null) {
            mi.setText(text);
        }
        if (icon != null) {
            mi.setImage(icon);
        }
        if (accel != -1) {
            mi.setAccelerator(accel);
        }
        mi.setEnabled(enabled);
        if (callback != null) {
            registerCallback(mi, this, callback, shell);
        }
        return mi;
    }

    protected void registerCallback(final MenuItem mi, final Object handler,
        final String handlerName, Shell shell) {
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {

                    Class[] cArg = {Shell.class};
                    Method m = handler.getClass().
                        getMethod(handlerName, cArg[0]);
                    m.invoke(handler, shell);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void doAbout(Shell shell) {
        MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
        mb.setText("BubbleBattle");
        mb.setMessage("Классная игра, неправда ли?");
        int rc = mb.open();
    }

    public void doExit(Shell shell) {
        System.exit(0);
    }

}
