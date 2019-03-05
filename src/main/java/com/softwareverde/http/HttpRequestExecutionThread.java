package com.softwareverde.http;

import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.util.IoUtil;
import com.softwareverde.util.ReflectionUtil;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.DataOutputStream;
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

    public HttpRequestExecutionThread(final String httpRequestUrl, final HttpRequest httpRequest, final HttpRequest.Callback callback, final Integer redirectCount) {
        _httpRequestUrl = httpRequestUrl;
        _httpRequest = httpRequest;
        _callback = callback;
        _redirectCount = redirectCount;
    }

    public void run() {
        try {
            final String urlString = _httpRequestUrl;
            final URL url = new URL((urlString) + (urlString.contains("?") ? "" : "?") + _httpRequest._queryString);

            final HttpURLConnection connection = (HttpURLConnection) (url.openConnection());

            if (connection instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

                if (! _httpRequest.validatesSslCertificates()) {
                    httpsConnection.setHostnameVerifier(HttpRequest.NAIVE_HOSTNAME_VERIFIER);

                    final SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, new X509TrustManager[]{ HttpRequest.NAIVE_TRUST_MANAGER }, new SecureRandom());
                    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                    httpsConnection.setSSLSocketFactory(sslSocketFactory);
                }
            }

            final Boolean followsRedirects = _httpRequest.followsRedirects();
            connection.setInstanceFollowRedirects(followsRedirects);
            connection.setDoInput(true);
            connection.setUseCaches(false);
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
            connection.setRequestProperty("Cookie", cookies.toString());

            final Map<String, String> httpRequestHeaders = _httpRequest._headers;
            for (final String key : httpRequestHeaders.keySet()) {
                final String value = httpRequestHeaders.get(key);
                connection.setRequestProperty(key, value);
            }

            final HttpMethod httpMethod = _httpRequest.getMethod();
            connection.setRequestMethod(httpMethod.name());

            if ((httpMethod == HttpMethod.POST) || (httpMethod == HttpMethod.PUT) || (httpMethod == HttpMethod.PATCH)) {
                final ByteArray postData = _httpRequest._postData;
                if (postData != null) {
                    connection.setDoOutput(true);

                    try (final DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                        out.write(postData.getBytes());
                        out.flush();
                    }
                }
            }

            connection.connect();

            final HttpResponse httpResponse = new HttpResponse();

            final int responseCode = connection.getResponseCode();
            httpResponse._responseCode = responseCode;
            httpResponse._responseMessage = connection.getResponseMessage();

            final Map<String, List<String>> responseHeaders = connection.getHeaderFields();
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
                            connection.disconnect();
                            System.out.println(newLocation);

                            (new HttpRequestExecutionThread(newLocation, _httpRequest, _callback, (_redirectCount + 1))).run();
                            return;
                        }
                    }
                }
            }

            final boolean upgradeToWebSocket = (_httpRequest.allowsWebSocketUpgrade() && HttpRequest.containsUpgradeToWebSocketHeader(responseHeaders));

            if (! upgradeToWebSocket) {
                if (responseCode >= 400) {
                    httpResponse._rawResult = MutableByteArray.wrap(IoUtil.readStreamOrThrow(connection.getErrorStream()));
                }
                else {
                    httpResponse._rawResult = MutableByteArray.wrap(IoUtil.readStreamOrThrow(connection.getInputStream()));
                }

                // Close Connection
                connection.disconnect();
            }
            else {
                try {
                    final HttpRequest.WebSocketFactory webSocketFactory = _httpRequest._webSocketFactory;
                    final Object httpConnectionHolder = ((connection instanceof HttpsURLConnection) ? ReflectionUtil.getValue(connection, "delegate") : connection);
                    final Object httpClient = ReflectionUtil.getValue(httpConnectionHolder, "http");
                    final Socket socket = ReflectionUtil.getValue(httpClient, "serverSocket");
                    httpResponse._webSocket = webSocketFactory.newWebSocket(socket);
                }
                catch (final Exception exception) {
                    System.out.println("NOTICE: Unable to get underlying socket for WebSocket within HttpRequest.");
                    exception.printStackTrace();
                    connection.disconnect();
                }
            }

            if (_callback != null) {
                _callback.run(httpResponse);
            }
        }
        catch (final Exception exception) {
            exception.printStackTrace();

            if (_callback != null) {
                _callback.run(null);
            }
        }
    }
}