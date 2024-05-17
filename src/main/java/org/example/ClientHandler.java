package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    static void defaultRequest(Request request, BufferedOutputStream out) {
        try {
            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);

            final var length = Files.size(filePath);
            out.write((
                    Response.OK.getMessage(mimeType, length)
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            Request request = Request.createRequest(requestLine);

            assert request != null;
            if (!Server.getValidPaths().contains(request.getPath())) {
                out.write((Response.NOT_FOUND.getMessage()).getBytes());
                out.flush();
                return;
            }

            if (Server.getHandlers().containsKey(request.getMethod())) {
                Map<String, Handler> handlerMap = Server.getHandlers().get(request.getMethod());
                if (handlerMap.containsKey(request.getPath())) {
                    Handler handler = handlerMap.get(request.getPath());
                    handler.handle(request, out);
                }
                defaultRequest(request, out);
            } else {
                defaultRequest(request, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
