package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Вместо метода для обработки конкретного подключения
 * создала отдельный класс ClientHandler
 */
public class Server{
    private final ExecutorService poolExecutor;

    public Server() {
        poolExecutor = Executors.newFixedThreadPool(64);
    }

    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                poolExecutor.execute(clientHandler);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}