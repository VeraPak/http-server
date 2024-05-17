package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    static Logger logger = MyLogger.getInstance().getLogger();
    private static ConcurrentMap<String, Map<String, Handler>> handlers;
    private final ExecutorService poolExecutor;

    public Server(int nThreads) {
        poolExecutor = Executors.newFixedThreadPool(nThreads);
        handlers = new ConcurrentHashMap<>();
        logger.log(Level.INFO, String.format("Создан новый сервер на %d потоков", nThreads));
    }

    public static ConcurrentMap<String, Map<String, Handler>> getHandlers() {
        return handlers;
    }

    public static List<String> getValidPaths() {
        return validPaths;
    }

    public void listen(int port) {
        logger.log(Level.INFO, String.format("Прослушиваем порт %d", port));
        try (final var serverSocket = new ServerSocket(port)) {
            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                poolExecutor.execute(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        Map<String, Handler> pathAndHandler = new HashMap<>();
        pathAndHandler.put(path, handler);
        handlers.put(method, pathAndHandler);
    }
}
