package com.softwareverde.http;

import com.softwareverde.util.IoUtil;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Set;

public class HttpRequest {
    private final String _baseUrl;
    private RequestMethod _requestMethod;
    private String _requestPath;
    private byte[] _requestData;
    private HashMap<String, String> _headers = new HashMap<>();

    public HttpRequest(final String baseUrl) {
        _baseUrl = baseUrl;
    }

    public RequestMethod getRequestMethod() {
        return _requestMethod;
    }

    public void setRequestMethod(final RequestMethod requestMethod) {
        _requestMethod = requestMethod;
    }

    public String getRequestPath() {
        return _requestPath;
    }

    public void setRequestPath(final String requestPath) {
        _requestPath = requestPath;
    }

    public byte[] getRequestData() {
        return _requestData;
    }

    /**
     * Sets binary data to be sent as the request data.
     * @param requestData
     */
    public void setRequestData(final byte[] requestData) {
        _requestData = requestData;
    }

    public void setRequestData(final String requestData) {
        _requestData = requestData.getBytes(Charset.forName("UTF-8"));
    }

    public void setRequestData(final JSONObject jsonObject) {
        setRequestData(jsonObject.toString());
    }

    public Set<String> getHeaderKeys() {
        return _headers.keySet();
    }

    public String getHeader(final String key) {
        return _headers.get(key);
    }

    public void putHeader(final String key, final String value) {
        _headers.put(key, value);
    }

    public HttpResponse send() throws IOException {
        final URL url = new URL(_buildUrl());
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(_requestMethod.name());
        for (final String header : _headers.keySet()) {
            final String headerValue = _headers.get(header);
            connection.setRequestProperty(header, headerValue);
        }

        final boolean doOutput = _requestData != null && _requestData.length > 0;
        connection.setDoOutput(doOutput);
        connection.setDoInput(true);
        if (doOutput) {
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(_requestData);
                outputStream.close();
            }
        }

        final int responseCode = connection.getResponseCode();
        final String responseMessage = connection.getResponseMessage();
        InputStream responseInputStream;
        try {
            responseInputStream = connection.getInputStream();
        }
        catch (final IOException exception) {
            // unable to open standard input stream, may have error data
            responseInputStream = connection.getErrorStream();
        }
        // responseInputStream may still be null
        byte[] responseBytes = null;
        if (responseInputStream != null) {
            responseBytes = IoUtil.readStreamOrThrow(responseInputStream);
        }

        return new HttpResponse(responseBytes, responseCode, responseMessage);
    }

    /**
     * Combines the base URL with the provided path.  Assumes the request path starts with a slash.
     * @return
     */
    protected String _buildUrl() {
        return _baseUrl + _requestPath;
    }

    public enum RequestMethod {
        GET,
        POST,
        DELETE,
        PATCH,
        PUT,
        OPTIONS
    }
}
