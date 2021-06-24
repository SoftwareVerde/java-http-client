package com.softwareverde.http;

import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.IoUtil;
import com.softwareverde.util.ReflectionUtil;
import com.softwareverde.util.Util;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

class HttpRequestExecutionThread extends Thread {
    protected final String _httpRequestUrl;
    protected HttpRequest _httpRequest;
    protected HttpRequest.Callback _callback;
    protected final Integer _redirectCount;
    protected HttpURLConnection _connection;

    protected Socket _extractConnectionSocket() {
        Object httpConnectionHolder = null;
        try {
            httpConnectionHolder = ((_connection instanceof HttpsURLConnection) ? ReflectionUtil.getValue(_connection, "delegate") : _connection);
            final Object httpClient = ReflectionUtil.getValue(httpConnectionHolder, "http");
            return ReflectionUtil.getValue(httpClient, "serverSocket");
        }
        catch (final Exception exception1) {
            if (httpConnectionHolder == null) {
                throw new RuntimeException("Unable to obtain connection socket via reflection", exception1);
            }
            try {
                // unable to get standard http server socket, check for OkHttp implementation
                final Object httpEngine = ReflectionUtil.getValue(httpConnectionHolder, "httpEngine");
                final Object streamAllocation = ReflectionUtil.getValue(httpEngine, "streamAllocation");
                final Object realConnection = ReflectionUtil.getValue(streamAllocation, "connection");
                return (Socket) ReflectionUtil.getValue(realConnection, "socket");
            }
            catch (final Exception exception2) {
                Logger.debug("Unable to get connection socket (1/2)", exception1);
                Logger.debug("Unable to get connection socket (2/2)", exception2);
                throw new RuntimeException("Unable to obtain connection socket via reflection");
            }
        }
    }

    protected void _configureRequestForWebSocketUpgrade() {
        _httpRequest.setAllowWebSocketUpgrade(true);
        _httpRequest.setHeader("Upgrade", "websocket");
    }

    public HttpRequestExecutionThread(final String httpRequestUrl, final HttpRequest httpRequest, final HttpRequest.Callback callback, final Integer redirectCount) {
        _httpRequestUrl = httpRequestUrl;
        _httpRequest = httpRequest;
        _callback = callback;
        _redirectCount = redirectCount;
    }

    public void run() {
        try {
            final String urlString;
            {
                final boolean isWebSocketRequest = _httpRequestUrl.startsWith("ws://") || _httpRequestUrl.startsWith("wss://");
                String requestUrl = _httpRequestUrl;
                if (isWebSocketRequest) {
                    requestUrl = requestUrl.replaceFirst("ws", "http");
                    _configureRequestForWebSocketUpgrade();
                }
                final String queryString = _httpRequest._queryString;
                if (! Util.isBlank(queryString)) {
                    urlString = (requestUrl + (requestUrl.contains("?") ? "" : "?") + queryString);
                }
                else {
                    urlString = requestUrl;
                }
            }

            final URL url = new URL(urlString);

            _connection = (HttpURLConnection) (url.openConnection());

            if (_connection instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsConnection = ((HttpsURLConnection) _connection);

                if (! _httpRequest.validatesSslCertificates()) {
                    httpsConnection.setHostnameVerifier(HttpRequest.NAIVE_HOSTNAME_VERIFIER);

                    final SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, new X509TrustManager[]{ HttpRequest.NAIVE_TRUST_MANAGER }, new SecureRandom());
                    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                    httpsConnection.setSSLSocketFactory(sslSocketFactory);
                }
            }

            final Boolean followsRedirects = _httpRequest.followsRedirects();
            _connection.setInstanceFollowRedirects(followsRedirects);
            _connection.setDoInput(true);
            _connection.setUseCaches(false);
            final StringBuilder cookies = new StringBuilder();
            {
                final List<String> httpRequestCookies = _httpRequest._cookies;
                String separator = "";
                for (final String cookie : httpRequestCookies) {
                    cookies.append(separator);
                    cookies.append(cookie);
                    separator = "; ";
                }
            }
            _connection.setRequestProperty("Cookie", cookies.toString());

            final Map<String, String> httpRequestHeaders = _httpRequest._headers;
            for (final String key : httpRequestHeaders.keySet()) {
                final String value = httpRequestHeaders.get(key);
                _connection.setRequestProperty(key, value);
            }

            final HttpMethod httpMethod = _httpRequest.getMethod();
            _connection.setRequestMethod(httpMethod.name());

            if ((httpMethod == HttpMethod.POST) || (httpMethod == HttpMethod.PUT) || (httpMethod == HttpMethod.PATCH)) {
                final ByteArray postData = _httpRequest._postData;
                if (postData != null) {
                    _connection.setDoOutput(true);

                    try (final DataOutputStream out = new DataOutputStream(_connection.getOutputStream())) {
                        out.write(postData.getBytes());
                        out.flush();
                    }
                }
            }

            _connection.connect();

            final HttpResponse httpResponse = new HttpResponse();

            final int responseCode = _connection.getResponseCode();
            httpResponse._responseCode = responseCode;
            httpResponse._responseMessage = _connection.getResponseMessage();

            final Map<String, List<String>> responseHeaders = _connection.getHeaderFields();
            httpResponse._headers = responseHeaders;

            // HttpURLConnection will not handle redirection from http to https, so it is is handled here...
            //  NOTE: Protocol switches will not be handled, except for http to https.  Downgrades from https to http will not be followed.
            if ( (followsRedirects) && (_redirectCount < _httpRequest._maxRedirectCount) ) {
                if (responseCode >= 300 && responseCode < 400) {
                    final String newLocation = HttpRequest.getHeaderValue("location", responseHeaders);
                    if (newLocation != null) {
                        final boolean isHttpBase = ( (_httpRequestUrl.startsWith("http") && (newLocation.startsWith("http"))) );
                        final boolean isHttpDowngrade = ( (_httpRequestUrl.startsWith("https")) && (! newLocation.startsWith("https")) );
                        if ( (isHttpBase) && (! isHttpDowngrade) ) {
                            _connection.disconnect();

                            (new HttpRequestExecutionThread(newLocation, _httpRequest, _callback, (_redirectCount + 1))).run();
                            return;
                        }
                    }
                }
            }

            final boolean upgradeToWebSocket = (_httpRequest.allowsWebSocketUpgrade() && HttpRequest.containsUpgradeToWebSocketHeader(responseHeaders));

            if (! upgradeToWebSocket) {
                if (responseCode >= 400) {
                    InputStream errorStream = null;
                    { // Attempt to obtain the errorStream, but fallback to the inputStream if errorStream is unavailable.
                        try {
                            errorStream = _connection.getErrorStream();
                        }
                        catch (final Exception exception) { }
                        if (errorStream == null) {
                            try {
                                errorStream = _connection.getInputStream();
                            }
                            catch (final Exception exception) { }
                        }
                    }

                    httpResponse._rawResult = ((errorStream != null) ? MutableByteArray.wrap(IoUtil.readStreamOrThrow(errorStream)) : null);
                }
                else {
                    final InputStream inputStream = _connection.getInputStream();
                    httpResponse._rawResult = ((inputStream != null) ? MutableByteArray.wrap(IoUtil.readStreamOrThrow(inputStream)) : null);
                }

                // Close Connection
                _connection.disconnect();
            }
            else {
                try {
                    final Socket socket = _extractConnectionSocket();
                    final HttpRequest.WebSocketFactory webSocketFactory = _httpRequest._webSocketFactory;
                    httpResponse._webSocket = webSocketFactory.newWebSocket(socket);
                }
                catch (final Exception exception) {
                    Logger.warn("Unable to get underlying socket for WebSocket within HttpRequest via reflection.", exception);
                    _connection.disconnect();
                }
            }

            _connection = null;
            if (_callback != null) {
                _callback.run(httpResponse);
            }
        }
        catch (final Exception exception) {
            Logger.debug("Unable to execute request.", exception);

            _connection = null;
            if (_callback != null) {
                _callback.run(null);
            }
        }
        finally {
            _connection = null;
        }
    }

    public void cancel() {
        final HttpURLConnection connection = _connection;
        if (connection == null) { return; }

        connection.disconnect();
    }

    public boolean isExecuting() {
        return (_connection != null);
    }
}