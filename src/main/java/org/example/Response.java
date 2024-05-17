package org.example;

public enum Response {
    NOT_FOUND("HTTP/1.1 404 Not Found\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n"),
    OK("HTTP/1.1 200 OK\r\n" +
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

    public String getMessage(String mimeType, long contentLength) {
        this.message = String.format(message, mimeType, contentLength);
        return message;
    }
}
