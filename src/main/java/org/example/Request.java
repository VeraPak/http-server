package org.example;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.http.NameValuePair;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Request {
    static Logger logger = MyLogger.getInstance().getLogger();
    private final String[] requestLine;
    private final List<String> headers;
    private final MultiValuedMap<String, Object> body;

    private final String method;
    private final String path;
    private final List<NameValuePair> queryParams;

    public Request(String[] requestLine, String method, String path, List<String> headers, MultiValuedMap<String, Object> body, List<NameValuePair> queryParams) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
    }

    public String[] getRequestLine() {
        return requestLine;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public MultiValuedMap<String, Object> getBody() {
        return body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQueryParams() {
        if (queryParams == null) {
            logger.log(Level.WARNING, "Query-параметры отсутствуют");
        }
        return queryParams;
    }

    public String getQueryParam(String name) {
        if (queryParams == null) {
            logger.log(Level.WARNING, "Query-параметр отсутствует");
            return null;
        }

        for (NameValuePair param : queryParams) {
            if (param.getName().equals(name)) {
                return param.getValue();
            }
        }
        logger.log(Level.WARNING, "Query-параметры не найден");
        return null;
    }

    public Collection<Object> getPostParam(String name){
        if(body == null) {
            logger.log(Level.WARNING, "Параметры отсутствуют в теле запроса");
            return null;
        }
        if(!body.containsKey(name)) {
            logger.log(Level.WARNING, "Параметр отсутствует в теле запроса");
            return null;
        }
        return body.get(name);
    }

    public MultiValuedMap<String, Object> getPostParams(){
        if(body == null) {
            logger.log(Level.WARNING, "Параметры отсутствуют в теле запроса");
            return null;
        }
        return body;
    }

    public Collection<Object> getPart(String name){
        if(body == null) {
            logger.log(Level.WARNING, "Параметры отсутствуют в теле запроса");
            return null;
        }
        if(!body.containsKey(name)) {
            logger.log(Level.WARNING, "Параметр отсутствует в теле запроса");
            return null;
        }
        return body.get(name);
    }

    public MultiValuedMap<String, Object> getParts() {
        if(body == null) {
            logger.log(Level.WARNING, "Параметры отсутствуют в теле запроса");
            return null;
        }
        return body;
    }
}
