package org.example;

public class Request {

    private String method;
    private String path;

    private Request(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public static Request createRequest(String requestLine) {

        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            // just close socket
            return null; //todo Exception
        }

        final var method = parts[0];
        final var path = parts[1];

        return new Request(method, path);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
