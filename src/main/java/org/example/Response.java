package org.example;

public enum Response {
    BAD_REQUEST("HTTP/1.1 400 Bad Request\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n"),

    NOT_FOUND("HTTP/1.1 404 Not Found\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n"),

    OK("HTTP/1.1 200 OK\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n"),

    OK_BY_PATH("HTTP/1.1 200 OK\r\n" +
            "Content-Type: %s\r\n" +
            "Content-Length: %d\r\n" +
            "Connection: close\r\n" +
            "\r\n");

    private String message;

    Response(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getMessage(String contentType, long contentLength) {
        this.message = String.format(message, contentType, contentLength);
        return message;
    }

//    public static void sendResponse(Path filePath, BufferedOutputStream out, Response response) {
//        try {
//            final var mimeType = Files.probeContentType(filePath);
//            final var length = Files.size(filePath);
//
//            out.write((
//                    response.getMessage(mimeType, length)
//            ).getBytes());
//            Files.copy(filePath, out);
//            out.flush();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void sendResponse(BufferedOutputStream out, Response response) {
//        try {
//            out.write((
//                    response.getMessage()
//            ).getBytes());
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
