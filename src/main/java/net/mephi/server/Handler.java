package net.mephi.server;

import net.mephi.client.components.Ball;
import net.mephi.client.components.BlackHole;
import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.Point;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Обработчик принятых от клиентов сообщений.
 * Занимается регистрацией новых клинетов, получением данных от зарегистрированных.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class Handler implements Runnable {
    private final SocketChannel _socketChannel;
    private final SelectionKey _selectionKey;

    private static final int READ_BUF_SIZE = 1024;
    private static final int WRiTE_BUF_SIZE = 1024;
    private ByteBuffer _readBuf = ByteBuffer.allocate(READ_BUF_SIZE);
    private ByteBuffer _writeBuf = ByteBuffer.allocate(WRiTE_BUF_SIZE);
    private Logger log = Logger.getLogger(Handler.class);
    private CheckCollissions col;


    public Handler(CheckCollissions col, Selector selector, SocketChannel socketChannel)
        throws IOException {
        this.col = col;

        _socketChannel = socketChannel;
        _socketChannel.configureBlocking(false);

        // Register _socketChannel with _selector listening on OP_READ events.
        // Callback: Handler, selected when the connection is established and ready for READ
        _selectionKey = _socketChannel.register(selector, SelectionKey.OP_READ);
        _selectionKey.attach(this);
        selector.wakeup(); // let blocking select() return
    }

    /**
     * Запускается из MultiSocketServer:111
     */
    public void run() {
        try {
            if (_selectionKey.isReadable()) {
                read();
            } else if (_selectionKey.isWritable()) {
                write();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Обработка входящих данных от клиентов
    synchronized void process() {
        _readBuf.flip();
        byte[] bytes = new byte[_readBuf.remaining()];
        _readBuf.get(bytes, 0, bytes.length);

        try {
            String s = new String(bytes);
            JSONParser parser = new JSONParser();
            JSONObject o = (JSONObject) parser.parse(s);
            log.debug("received " + o.toString());

            if (o.get("type").equals("register")) {
                log.debug("New client connected");

                //читаем имя, цвет и размер экрана
                Ball b = new Ball((String) o.get("name"), Ball.START_CLIENT_RADIUS);

                b.getUserField().width = ((Long) o.get("width")).intValue();
                b.getUserField().height = ((Long) o.get("height")).intValue();
                JSONObject color = (JSONObject) o.get("color");
                b.setColor(new Color(((Long) color.get("red")).intValue(),
                    ((Long) color.get("green")).intValue(), ((Long) color.get("blue")).intValue()));
                b.setRandomFieldPosition();


                Client c = new Client(null);
                UUID uuid = UUID.randomUUID();//Уникальный номер клиента
                c.setUUID(uuid.toString());
                c.setBall(b);

                //отправляем уникальный ID  и глобальные координаты поля
                JSONObject answer = new JSONObject();
                answer.put("type", "register_complete");
                answer.put("uuid", c.getUUID());
                answer.put("field.x", b.getUserField().x);
                answer.put("field.y", b.getUserField().y);
                String resp = answer.toString() + '\n';

                log.debug("send " + answer.toString());
                _writeBuf = ByteBuffer.wrap(resp.getBytes());


                col.addClient(c);
                log.debug("registered new Client: " + c);

            } else if (o.get("type").equals("cursor")) {
                //Пришли новые координаты поля от клиента
                col.updateClient((String) o.get("uuid"),
                    new Point(((Long) o.get("x")).intValue(), ((Long) o.get("y")).intValue()));
                //отправляем все объекты
                JSONObject answer = getAllResponceData((String) o.get("uuid"));
                String resp = answer.toString() + '\n';

                log.debug("send " + resp);
                _writeBuf = ByteBuffer.wrap(resp.getBytes());
            } else if (o.get("type").equals("shutdown")) {

                col.getClientsList4Delete().add((String) o.get("uuid"));
                _selectionKey.cancel();
                return;
            }
        } catch (ParseException p) {
        }



        // Set the key's interest to WRITE operation
        _selectionKey.interestOps(SelectionKey.OP_WRITE);
        _selectionKey.selector().wakeup();
    }

    synchronized void read() throws IOException {
        try {
            int numBytes = _socketChannel.read(_readBuf);

            if (numBytes == -1) {
                _selectionKey.cancel();
                _socketChannel.close();
                log.debug("read(): client connection might have been dropped!");
            } else {
                MultipleSocketServer.getWorkerPool().execute(() -> process());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            _selectionKey.cancel();
            _selectionKey.channel().close();
            return;
        }
    }

    void write() throws IOException {
        int numBytes = 0;

        try {
            numBytes = _socketChannel.write(_writeBuf);
            //            System.out.println("write(): #bytes read from '_writeBuf' buffer = " + numBytes);
            if (numBytes > 0) {
                _readBuf.clear();
                _writeBuf.clear();

                // Set the key's interest-set back to READ operation
                _selectionKey.interestOps(SelectionKey.OP_READ);
                _selectionKey.selector().wakeup();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            _selectionKey.cancel();
            _socketChannel.close();
        }
    }

    /**
     * Пересылаем еду и клиентов, в пределах видимости клиента
     * И Top5 игроков
     *
     * @param uuid id клиента, которому будут отправлены данные
     * @return Объект пересылки
     */
    private JSONObject getAllResponceData(String uuid) {
        List<Client> clients = col.getClientsList();
        Client responceTo = null;
        for (Client c : clients) {
            if (c.getUUID().equals(uuid)) {
                responceTo = c;
                break;
            }
        }


        JSONObject response = new JSONObject();
        response.put("type", "refresh");
        response.put("food", getFoodArray(col.getFoodList(), responceTo));
        response.put("blackhole", getHolesArray(col.getHoles(), responceTo));
        response.put("clients", getClientsArray(clients, responceTo));
        response.put("top5", getTop5Array(clients));

        return response;
    }

    public JSONArray getFoodArray(Ball[] food, Client responceTo) {
        JSONArray arrayFood = new JSONArray();
        for (Ball b : food) {
            if (b.isVisible() && responceTo.getBall().isBallInCurrentField(b)) {
                JSONObject foodCoord = new JSONObject();
                foodCoord.put("x",
                    b.getCenterGlobal().x - responceTo.getBall().getLeftTopFieldPosition().x);
                foodCoord.put("y",
                    b.getCenterGlobal().y - responceTo.getBall().getLeftTopFieldPosition().y);

                JSONObject color = new JSONObject();
                color.put("red", b.getColor().getRed());
                color.put("green", b.getColor().getGreen());
                color.put("blue", b.getColor().getBlue());
                foodCoord.put("color", color);

                arrayFood.add(foodCoord);
            }

        }
        return arrayFood;
    }

    public JSONArray getHolesArray(BlackHole[] holes, Client responceTo) {
        JSONArray arrayHoles = new JSONArray();
        for (BlackHole hole : holes) {
            if (responceTo.getBall().isBallInCurrentField(hole)) {
                JSONObject holeCoord = new JSONObject();
                holeCoord.put("x", hole.getCenterGlobalPosition().x - responceTo.getBall()
                    .getLeftTopFieldPosition().x);
                holeCoord.put("y", hole.getCenterGlobalPosition().y - responceTo.getBall()
                    .getLeftTopFieldPosition().y);
                holeCoord.put("id", hole.getImageNumber());
                arrayHoles.add(holeCoord);
            }
        }
        return arrayHoles;
    }

    public JSONArray getClientsArray(List<Client> clients, Client responceTo) {
        JSONArray arrayClient = new JSONArray();
        for (Client c : clients) {
            if (responceTo.getBall().isBallInCurrentField(c.getBall())) {
                JSONObject balls = new JSONObject();
                balls.put("x", c.getBall().getCenterGlobalPosition().x - responceTo.getBall()
                    .getLeftTopFieldPosition().x);
                balls.put("y", c.getBall().getCenterGlobalPosition().y - responceTo.getBall()
                    .getLeftTopFieldPosition().y);
                balls.put("uuid", c.getUUID());
                balls.put("radius", c.getBall().getRadius());
                balls.put("name", c.getBall().getName());

                JSONObject color = new JSONObject();
                color.put("red", c.getBall().getColor().getRed());
                color.put("green", c.getBall().getColor().getGreen());
                color.put("blue", c.getBall().getColor().getBlue());
                balls.put("color", color);
                arrayClient.add(balls);
            }
        }
        return arrayClient;
    }

    public JSONArray getTop5Array(List<Client> clients) {
        //Top5
        JSONArray arrayClientTop5 = new JSONArray();
        Collections.sort(clients, new Comparator<Client>() {
            @Override public int compare(Client o1,
                Client o2) { //сортировка массива клиентов по убыванию радиуса
                return (int) Math.signum(o1.getBall().getRadius() - o2.getBall().getRadius());
            }
        });
        for (int i = 0; i < Math.min(5, clients.size()); i++) {
            JSONObject ball = new JSONObject();
            ball.put("name", clients.get(i).getBall().getName());
            ball.put("score", clients.get(i).getBall().getRadius());
            arrayClientTop5.add(ball);
        }

        return arrayClientTop5;
    }
}
