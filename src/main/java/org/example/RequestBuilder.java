package org.example;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.fileupload.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestBuilder {
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final List<String> allowedMethods = List.of(GET, POST);
    private static final byte[] delimiter = new byte[]{'\r', '\n'};
    private static final byte[] doubleDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
    private static final Logger logger = MyLogger.getInstance().getLogger();

    private static String[] requestLine;
    private static List<String> headers;
    private static MultiValuedMap<String, Object> body;
    private static String method;
    private static String path;
    private static List<NameValuePair> query;

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
            if (requestLine.length != 3) {
                logger.log(Level.WARNING, String.format("Элементов в строке : %d", requestLine.length));
                return null;
            }

            //method
            method = requestLine[0];
            if (!allowedMethods.contains(method)) {
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
            if (path.contains("?")) {
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
            if (headersEnd == -1) {
                logger.log(Level.WARNING, "Заголовки не найдены");
                return null;
            }

            in.reset();
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            logger.log(Level.INFO, String.format("Заголовки: %s", headers));

            //body
            if (!method.equals(GET)) {
                in.skip(doubleDelimiter.length);

                final var contentLength = extractHeader(headers, "Content-Length");
                final var contentType = extractHeader(headers, "Content-Type");

                if (contentLength.isPresent() && contentType.isPresent()) {
                    body = new ArrayListValuedHashMap<>();
                    final var length = Integer.parseInt(contentLength.get());

                    if (contentType.get().startsWith("text/plain")) {
                        final var bodyBytes = in.readNBytes(length);
                        var bodyText = new String(bodyBytes);
                        for (String line : bodyText.split("\r\n")) {
                            String[] keyValue = line.split("=", 2);
                            String key = keyValue[0].trim();
                            String value = keyValue[1].trim();
                            body.put(key, value);
                        }
                    } else if(contentType.get().startsWith("application/x-www-form-urlencoded")){
                        final var bodyBytes = in.readNBytes(length);
                        var bodyText = new String(bodyBytes);
                        var parsedBody = URLEncodedUtils.parse(bodyText, StandardCharsets.UTF_8);
                        for (NameValuePair line : parsedBody) {
                            String key = line.getName().trim();
                            String value = line.getValue().trim();
                            body.put(key, value);
                        }
                    } else if(contentType.get().startsWith("multipart/form-data")) {
                        try {
                            var request = new MyRequestContext("utf-8", contentType.get(), length, in); //TODO in уже прочитан

                            DiskFileItemFactory factory = new DiskFileItemFactory();
                            factory.setSizeThreshold(100);
                            factory.setRepository(new File("temp"));

                            ServletFileUpload upload = new ServletFileUpload(factory);

                            List<FileItem> items = upload.parseRequest(request);
                            Iterator<FileItem> iterator = items.iterator();
                            while (iterator.hasNext()){
                                FileItem item = iterator.next();
                                if (item.isFormField()) {
                                    body.put(item.getFieldName(), item.getString("utf-8"));
                                } else {
                                    body.put(item.getFieldName(), item);
                                }
                            }
                        } catch (FileUploadException e) {
                            logger.log(Level.WARNING, String.format("Ошибка при попытке распарсить multipart/form-data %s", e.getMessage()));
                        }
                    } else {
                        logger.log(Level.WARNING, String.format("Content-Type %s отсутствует", contentType.get()));
                    }
                }
                logger.log(Level.INFO, String.format("Тело запроса: %s", body));
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

class MyRequestContext implements RequestContext {
    private String characterEncoding;
    private String contentType;
    private int contentLength;
    private InputStream inputStream;

    public MyRequestContext(String characterEncoding, String contentType, int contentLength, InputStream inputStream) {
        this.characterEncoding = characterEncoding;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.inputStream = inputStream;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }
}