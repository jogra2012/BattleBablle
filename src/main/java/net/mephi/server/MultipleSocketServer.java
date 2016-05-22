package net.mephi.server;

import net.mephi.client.components.Ball;
import net.mephi.client.components.Board;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Стартовый класс сервера.
 * Создает NIO обработчик сообщений и впадает в бесконечный цикл по их получению.
 *
 * @author Julia
 * @since 01.01.2016
 */
public class MultipleSocketServer implements Runnable {

    private final Selector _selector;
    private final ServerSocketChannel _serverSocketChannel;
    private static final int WORKER_POOL_SIZE = 10;
    private static ExecutorService _workerPool;


    private Logger log = Logger.getLogger(MultipleSocketServer.class);




    public MultipleSocketServer(int port) throws IOException {
        CheckCollissions col =
            new CheckCollissions();//Запускает отдельный поток пересчета столкновений
        log.debug("Server started on " + port);
        Thread t = new Thread(col);
        t.setName("check collissions thread");
        t.start();

        _selector = Selector.open();
        _serverSocketChannel = ServerSocketChannel.open();
        _serverSocketChannel.socket().bind(new InetSocketAddress(port));
        _serverSocketChannel.configureBlocking(false);

        // Register _serverSocketChannel with _selector listening on OP_ACCEPT events.
        // Callback: Acceptor, selected when a new connection incomes.
        //Регистрируем сокет-канал, который слушает OP_ACCEPT события
        //При возникновении события вызывается new Acceptor();
        SelectionKey selectionKey =
            _serverSocketChannel.register(_selector, SelectionKey.OP_ACCEPT);
        selectionKey.attach(new Acceptor(col));

    }

    public static void main(String[] args) {

        String port = args.length > 0 ? args[0] : "6999";
        try {
            _workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);

            MultipleSocketServer server = new MultipleSocketServer(Integer.parseInt(port));
            Thread t = new Thread(server);
            t.setName("Multiserver thread");
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    // Acceptor: На каждое новое соединение (Сервер - клиент) цепляет обработчик - класс Handler
    private class Acceptor implements Runnable {
        CheckCollissions col;

        public Acceptor(CheckCollissions col) {
            this.col = col;
        }

        public void run() {
            try {
                SocketChannel socketChannel = _serverSocketChannel.accept();
                if (socketChannel != null) {
                    new Handler(col, _selector, socketChannel);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void run() {
        try {
            // Бесконечный цикл работы сервера
            while (true) {
                _selector.select();
                Iterator it = _selector.selectedKeys().iterator();//Итератор по клиентам

                while (it.hasNext()) {
                    SelectionKey sk = (SelectionKey) it.next();
                    if (sk.isValid()) {
                        it.remove();
                        Runnable r =
                            (Runnable) sk.attachment(); // handler or acceptor callback/runnable
                        if (r != null) {
                            r.run();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static ExecutorService getWorkerPool() {
        return _workerPool;
    }



}
