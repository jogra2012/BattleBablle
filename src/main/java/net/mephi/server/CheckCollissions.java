package net.mephi.server;

import net.mephi.client.components.Ball;
import net.mephi.client.components.BlackHole;
import net.mephi.client.components.Board;
import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Класс пересчета столкновений.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class CheckCollissions implements Runnable {

    private Logger log = Logger.getLogger(MultipleSocketServer.class);


    private List<Client> clientsList = new ArrayList<>();//Основной список, где хранятся все клиенты

    private List<String> clientList4Delete = new ArrayList<>();
    private Map<String, Point> clients4Update = new HashMap<>();
    private List<Client> clientList4Register = new ArrayList<>();
    private Ball[] foods = new Ball[Board.MAX_FOOD_AMOUNT];
    private BlackHole[] holes = new BlackHole[Board.BLACK_HOLES_AMOUNT];
    public Object lock = new Object();

    public CheckCollissions() {
        //расставляем черные дыры на поле.
        locateBlackHoles();

        //Инициализация массива еды
        locateFood();
    }

    public void run() {
        while (true) {
            while (clientsList.size() > 0 || clientList4Register.size() > 0) {
                clientsList
                    .addAll(clientList4Register);//добавить только что зарегистрированных клиентов
                clientList4Register.clear();

                for (Iterator<Client> iterator = clientsList.iterator(); iterator.hasNext(); ) {
                    //Обновить данные клиентов
                    Client c = iterator.next();
                    if (clients4Update.containsKey(c.getUUID())) {
                        c.getBall().setUserFieldPosition(clients4Update.get(c.getUUID()));
                    }
                    //Убрать отключившихся клиентов
                    if (clientList4Delete.contains(c.getUUID())) {
                        iterator.remove();
                    }
                }
                clients4Update.clear();

                //Закончили обновлять clientsList

                checkCollissions();
            }
            //Если нет ни одного клиента, сервер спит.
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public void addClient(Client c) {
        clientList4Register.add(c);
        synchronized (lock) {
            lock.notify();//Если сервер спал, разбудить.
        }

    }

    public List<String> getClientsList4Delete() {
        return clientList4Delete;
    }

    public List<Client> getClientsList() {
        List<Client> l = new ArrayList<>(clientsList);
        return l;
    }

    /**
     * Метод проверки столкновений
     */
    public void checkCollissions() {
        //столкновение клиентов и черных дыр
        for (BlackHole blackHole : holes) {
            for (Client client : clientsList) {
                if (blackHole.checkCollisionTo(client.getBall())) {
                    blackHole.decreaseMassOf(client.getBall());
                    log.debug("Client " + client + " near BlackHole " + blackHole);
                }
            }
        }

        //столкновения клиентов и еды
        for (Ball food : foods) {
            if (food.isVisible()) {
                for (Client client : clientsList) {
                    if (client.getBall().checkCollisionTo(food)) {
                        food.setVisible(false);

                        client.getBall().increaseMass(food);
                        log.debug("Client " + client + " eated");
                    }
                }
            }
        }

        //Съеденную еду перенести на новое место
        for (Ball food : foods) {
            if (!food.isVisible()) {
                food.setRandomCenterPosition();
                food.setVisible(true);
            }
        }

        //пересечение клиентов друг с другом
        for (Client client1 : clientsList) {
            for (Client client2 : clientsList) {
                if (client1.getBall().isVisible() && client2.getBall().isVisible() && client1
                    .getBall().isThisCanIt(client2.getBall()) && !client1.getUUID()
                    .equals(client2.getUUID())) {

                    client1.getBall().increaseMass(client2.getBall());
                    client2.getBall().setRadius(Ball.END_GAME_RADIUS);
                    client2.getBall().setVisible(false);

                    log.debug("Client " + client1 + " ate Client " + client2);
                }
            }
        }


    }

    public void locateFood() {
        log.debug("MAX_FOOD_AMOUNT=" + Board.MAX_FOOD_AMOUNT);
        for (int i = 0; i < Board.MAX_FOOD_AMOUNT; i++) {
            Ball f = new Ball(Ball.FOOD_NAME, Ball.FOOD_RADIUS);
            f.setRandomColor();
            f.setRandomCenterPosition();
            f.setVisible(true);
            f.setFood(true);
            foods[i] = f;
        }
    }

    public void locateBlackHoles() {
        log.debug("Black holes amount = " + Board.BLACK_HOLES_AMOUNT);
        for (int i = 0; i < Board.BLACK_HOLES_AMOUNT; i++) {

            BlackHole b1 = new BlackHole(i + 1);
            b1.setUserFieldPosition(b1.setRandomCenterPosition());
            holes[i] = b1;
        }
    }

    public Ball[] getFoodList() {
        return foods;
    }

    public BlackHole[] getHoles() {
        return holes;
    }

    public void updateClient(String uuid, Point field) {
        clients4Update.put(uuid, field);
    }
}
