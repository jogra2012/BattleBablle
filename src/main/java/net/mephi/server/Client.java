package net.mephi.server;

import net.mephi.client.components.Ball;
import net.mephi.client.components.Board;
import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.Point;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Клиент. Хранит информацию о шаре.
 * Содержит поток для обмена сообщений с сервером.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class Client {
    private Socket socket = null;

    private Client thisClient = null;
    private DataOutputStream oos;
    private BufferedReader ois;
    private Ball ball = null;
    private String uuid;
    private Board board;
    private boolean isActive = true;
    public static final String DATA_REQUEST = "DATA_REQUEST";
    private Logger log = Logger.getLogger(MultipleSocketServer.class);

    public Client() {

    }

    public void setClient(Client c) {
        this.thisClient = c;

    }

    public Client(Socket s) {
        this.socket = s;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public String getUUID() {
        return uuid;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setBall(Ball ball) {
        this.ball = ball;
    }

    public Ball getBall() {
        return ball;
    }



    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setBoard(Board b) {
        this.board = b;
    }

    public void endGame() {
        this.isActive = false;
    }

    public void startGame() {
        //Запуск потока для обмена сообщениями с сервером
        Thread thread1 = new Thread(new Runnable() {
            @Override public void run() {

                try {
                    DataOutputStream outToServer = getOos();
                    BufferedReader inFromServer = getOis();
                    JSONParser parser = new JSONParser();
                    long time4OneCycle = 0;

                    while (isActive) {

                        long t1 = System.currentTimeMillis();
                        //Отправить свой id и новые координаты поля, относительно глобального
                        Point p = board.getCursorLocation();
                        ball.setCursorLocation(p);
                        ball.countNewFieldPosition(p);

                        JSONObject obj = new JSONObject();
                        obj.put("type", "cursor");
                        obj.put("uuid", getUUID());
                        obj.put("x", ball.getUserField().x);
                        obj.put("y", ball.getUserField().y);

                        log.debug("send " + obj.toString());
                        outToServer
                            .write(obj.toString().getBytes());//Отправка нового положения на сервер
                        try {
                            //ожидать обработанных данных
                            String res = inFromServer.readLine();
                            JSONObject o = (JSONObject) parser.parse(res);//Парсим JSON
                            log.debug("receive " + o.toString());
                            if (o.get("type").equals("refresh")) {
                                //перерисовать доску

                                board.refreshBoard(o, ball.getLinesShift(), thisClient,
                                    time4OneCycle);
                            }
                        } catch (ParseException e) {
                            log.error(e);
                        }


                        long t2 = System.currentTimeMillis();
                        time4OneCycle = t2 - t1; //Время в миллисекундах, затраченное на один цикл
                        log.debug("general Time =" + time4OneCycle);
                        if (time4OneCycle < 20) {
                            try {
                                time4OneCycle = 20 - time4OneCycle;
                                Thread.sleep(20
                                    - time4OneCycle);//Текущий поток спит, чтобы не чаще раз 20 миллисекунд опрашивать сервер
                            } catch (InterruptedException e) {
                            }
                        }
                    }

                    JSONObject obj = new JSONObject();
                    obj.put("type", "shutdown");
                    obj.put("uuid", getUUID());
                    log.debug("send " + obj.toString());
                    outToServer.write(obj.toString().getBytes());

                    oos.close();//Закрыть соединение
                    ois.close();
                    getSocket().close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        thread1.setName("Client main thread");
        thread1.start();

    }

    public DataOutputStream getOos() {
        return oos;
    }

    public void setOos(DataOutputStream oos) {
        this.oos = oos;
    }

    public BufferedReader getOis() {
        return ois;
    }

    public void setOis(BufferedReader ois) {
        this.ois = ois;
    }

    @Override public String toString() {
        return "<" + getUUID() + " / " + getBall().getName() + ">";
    }

}
