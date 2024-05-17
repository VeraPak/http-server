package org.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = MyLogger.getInstance().getLogger();
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (final var in = new BufferedInputStream(socket.getInputStream());
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var limit = 4096;

            Request request = RequestBuilder.build(in, limit);
            if (request == null) {
                try {
                    out.write((
                            Response.BAD_REQUEST.getMessage()
                    ).getBytes());
                    out.flush();
                } catch (IOException e) {
                    logger.log(Level.WARNING, String.format("Ошибка при отправке ответа клиенту: %s", e.getMessage()));
                }
                return;
            }

            System.out.println("ПАРАМЕТРЫ в теле: " + request.getPostParams());
            System.out.println("ПАРАМЕТР в теле: " + request.getPostParam("title"));

            if (!Server.getValidPaths().contains(request.getPath())) {
                try {
                    out.write((
                            Response.OK.getMessage()
                    ).getBytes());
                    out.flush();
                } catch (IOException e) {
                    logger.log(Level.WARNING, String.format("Ошибка при отправке ответа клиенту: %s", e.getMessage()));
                }
                return;
            }

            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);

            if (Server.getHandlers().containsKey(request.getMethod())) {
                Map<String, Handler> handlerMap = Server.getHandlers().get(request.getMethod());
                if (handlerMap.containsKey(request.getPath())) {
                    Handler handler = handlerMap.get(request.getPath());
                    handler.handle(request, out);
                }
                try {
                    out.write((
                            Response.OK_BY_PATH.getMessage(mimeType, length)
                    ).getBytes());
                    Files.copy(filePath, out);
                    out.flush();
                } catch (IOException e) {
                    logger.log(Level.WARNING, String.format("Ошибка при отправке ответа клиенту: %s", e.getMessage()));
                }
            }

        } catch (IOException e) {
            logger.log(Level.WARNING, String.format("Ошибка при обработке клиента: %s", e.getMessage()));
        }
    }
}
