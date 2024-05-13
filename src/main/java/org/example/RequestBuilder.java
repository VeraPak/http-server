package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestBuilder {
    static Logger logger = MyLogger.getInstance().getLogger();

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final List<String> allowedMethods = List.of(GET, POST);

    private static final byte[] delimiter = new byte[]{'\r', '\n'};
    private static final byte[] doubleDelimiter = new byte[]{'\r', '\n', '\r', '\n'};

    private static String[] requestLine;
    private static List<String> headers;
    private static String body;
    private static String method;
    private static String path;
    private static List<NameValuePair> query;

    //TODO убрать static у createRequest() ?
    public static Request build(BufferedInputStream in, int limit) {
        try {
            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            //request line
            final var requestLineEnd = indexOf(buffer, delimiter, 0, read);
            if (requestLineEnd == -1) {
                logger.log(Level.WARNING, "Request line не найден");
                return null;
            }

            requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if(requestLine.length != 3){
                logger.log(Level.WARNING, String.format("Элементов в строке : %d", requestLine.length));
                return null;
            }

            //method
            method = requestLine[0];
            if(!allowedMethods.contains(method)){
                logger.log(Level.WARNING, String.format("Недопустимый метод: %s", method));
                return null;
            }
            logger.log(Level.INFO, String.format("Метод: %s", method));

            //path
            path = requestLine[1];
            if (!path.startsWith("/")) {
                logger.log(Level.WARNING, String.format("Недопустимый путь: %s", path));
                return null;
            }
            if(path.contains("?")) {
                final var pathEnd = indexOf(buffer, new byte[]{'?'}, 0, requestLineEnd);
                path = new String(Arrays.copyOfRange(buffer, method.length() + 1, pathEnd));

                //version
                var version = requestLine[2];

                //query
                var queryString = new String(Arrays.copyOfRange(buffer, pathEnd + 1, requestLineEnd - version.length()));
                query = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
            }
            logger.log(Level.INFO, String.format("Путь: %s", path));
            logger.log(Level.INFO, String.format("Query: %s", query));

            //headers
            final var headersStart = requestLineEnd + doubleDelimiter.length;
            final var headersEnd = indexOf(buffer, doubleDelimiter, headersStart, read);
            if(headersEnd == -1) {
                logger.log(Level.WARNING, "Заголовки не найдены");
                return null;
            }

            in.reset();
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd-headersStart);
            headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            logger.log(Level.INFO, String.format("Заголовки: %s", headers));

            //body
            if (!method.equals(GET)) {
                in.skip(doubleDelimiter.length);

                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);

                    body = new String(bodyBytes);
                    logger.log(Level.INFO, String.format("Тело запроса: %s", body));
                }
            }
            return new Request(requestLine, method, path, headers, body, query);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
