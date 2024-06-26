package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    static final int N_THREADS = 64;
    static final int PORT = 9999;
    static Logger logger = MyLogger.getInstance().getLogger();

    public static void main(String[] args) {
        final var server = new Server(N_THREADS);

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            // TODO: handlers code
        });
        server.addHandler("POST", "/messages", (request, responseStream) -> {
            // TODO: handlers code
        });

        server.addHandler("GET", "/classic.html", (request, responseStream) -> {
            try {
                final var filePath = Path.of(".", "public", request.getPath());
                final var mimeType = Files.probeContentType(filePath);

                final var template = Files.readString(filePath);
                final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
                responseStream.write((
                        Response.OK_BY_PATH.getMessage(mimeType, content.length)
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        });

        server.listen(PORT);
    }
}