package server;

import server.auth.AuthService;
import server.collection.CollectionManager;
import server.commands.CommandInvoker;
import server.concurrent.ThreadPoolManager;
import server.db.PersonDao;
import server.handler.RequestProcessorThread;
import shared.network.PacketSplitter;
import shared.network.Request;
import shared.network.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final int PORT = 8081;
    private static final int MAX_PACKET_SIZE = 65507;
    private static final int POOL_PARALLELISM = 4;

    private final DatagramChannel channel;
    private final Selector selector;
    private final CollectionManager collectionManager;
    private final CommandInvoker commandInvoker;
    private final PersonDao personDao;
    private final AuthService authService;
    private final ThreadPoolManager threadPoolManager;

    private final ConcurrentHashMap<SocketAddress, String> sessions = new ConcurrentHashMap<>();

    private volatile boolean running;

    public Server(CollectionManager cm, PersonDao dao, AuthService auth) throws IOException {
        this.collectionManager = cm;
        this.personDao = dao;
        this.authService = auth;
        this.commandInvoker = new CommandInvoker(cm);
        this.threadPoolManager = new ThreadPoolManager(POOL_PARALLELISM);
        this.running = true;
        this.channel = initializeChannel();
        this.selector = initializeSelector();
    }

    private DatagramChannel initializeChannel() throws IOException {
        DatagramChannel ch = DatagramChannel.open();
        ch.configureBlocking(false);
        ch.bind(new InetSocketAddress(PORT));
        ch.socket().setReceiveBufferSize(1024 * 1024);
        ch.socket().setSendBufferSize(1024 * 1024);
        return ch;
    }

    private Selector initializeSelector() throws IOException {
        Selector sel = Selector.open();
        channel.register(sel, SelectionKey.OP_READ);
        return sel;
    }

    public void start() {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
        logger.info("Сервер запущен на порту " + PORT + " (многопоточный режим)");
        System.out.println("[ЗАПУСК] Сервер ожидает подключений клиентов...");

        while (running) {
            try {
                selector.select(100);
                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isReadable() && key.channel() == channel) {
                        handleClientRequest(buffer);
                    }
                }
                selector.selectedKeys().clear();
            } catch (IOException e) {
                if (running) logger.severe("Ошибка селектора: " + e.getMessage());
            }
        }
    }

    private void handleClientRequest(ByteBuffer buffer) throws IOException {
        buffer.clear();
        SocketAddress clientAddress = channel.receive(buffer);
        if (clientAddress == null) return;

        buffer.flip();
        byte[] receivedData = new byte[buffer.remaining()];
        buffer.get(receivedData);

        threadPoolManager.submitRead(() -> {
            try {
                Request request;
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(receivedData))) {
                    request = (Request) ois.readObject();
                }
                logger.info("Получен запрос: " + request.getCommandName() + " от " + clientAddress);

                new RequestProcessorThread(
                        request, clientAddress, channel,
                        collectionManager, personDao, authService, commandInvoker,
                        threadPoolManager.getWritePool(),
                        sessions
                ).start();

            } catch (IOException | ClassNotFoundException e) {
                logger.severe("Ошибка десериализации: " + e.getMessage());
                sendErrorResponse(clientAddress, "Ошибка формата запроса");
            }
        });
    }

    private void sendResponseAsync(Response response, SocketAddress clientAddress) {
        threadPoolManager.submitWrite(() -> {
            try {
                List<byte[]> packets = PacketSplitter.split(response);
                for (byte[] packet : packets) {
                    channel.send(ByteBuffer.wrap(packet), clientAddress);
                }
                channel.send(ByteBuffer.wrap(new byte[]{0}), clientAddress);
                logger.info("Ответ отправлен клиенту " + clientAddress);
            } catch (IOException e) {
                logger.severe("Ошибка отправки ответа: " + e.getMessage());
            }
        });
    }

    private void sendErrorResponse(SocketAddress clientAddress, String message) {
        Response error = new Response.Builder().success(false).message(message).build();
        sendResponseAsync(error, clientAddress);
    }

    public void stop() {
        running = false;
        try { selector.wakeup(); } catch (Exception ignored) {}
        threadPoolManager.shutdown();
        logger.info("Сервер остановлен");
    }

    public void close() {
        try {
            if (selector.isOpen()) selector.close();
            if (channel.isOpen()) channel.close();
        } catch (IOException e) {
            logger.severe("Ошибка закрытия ресурсов: " + e.getMessage());
        }
    }
}