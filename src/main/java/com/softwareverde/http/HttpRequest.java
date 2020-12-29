package com.softwareverde.http;

import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.http.websocket.ConnectionLayer;
import com.softwareverde.http.websocket.WebSocket;
import com.softwareverde.util.Container;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.Util;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class HttpRequest {
    public interface Callback {
        void run(HttpResponse response);
    }

    public interface WebSocketFactory {
        WebSocket newWebSocket(Socket socket);
    }

    public static class DefaultWebSocketFactory implements WebSocketFactory {
        private static final AtomicLong NEXT_WEB_SOCKET_ID = new AtomicLong(1L);

        @Override
        public WebSocket newWebSocket(final Socket socket) {
            final Long webSocketId = NEXT_WEB_SOCKET_ID.getAndIncrement();
            return new WebSocket(webSocketId, WebSocket.Mode.CLIENT, ConnectionLayer.newConnectionLayer(socket), WebSocket.DEFAULT_MAX_PACKET_BYTE_COUNT);
        }
    }

    protected static final HostnameVerifier NAIVE_HOSTNAME_VERIFIER = new HostnameVerifier() {
        @Override
        public boolean verify(final String hostname, final SSLSession sslSession) { return true; }
    };

    protected static final X509ExtendedTrustManager NAIVE_TRUST_MANAGER = new NaiveTrustManager();

    public static boolean containsHeaderValue(final String key, final String value, final Map<String, List<String>> headers) {
        for (final String headerKey : headers.keySet()) {
            if (Util.areEqual(Util.coalesce(key).toLowerCase(), Util.coalesce(headerKey).toLowerCase())) {
                for (final String headerValue : headers.get(headerKey)) {
                    if (Util.areEqual(Util.coalesce(value).toLowerCase(), Util.coalesce(headerValue).toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Set<String> getHeaderKeys() {
        return Util.copySet(_headers.keySet());
    }

    public static String getHeaderValue(final String key, final Map<String, List<String>> headers) {
        for (final String headerKey : headers.keySet()) {
            if (Util.areEqual(Util.coalesce(key).toLowerCase(), Util.coalesce(headerKey).toLowerCase())) {
                final List<String> headerValues = headers.get(headerKey);
                if (headerValues.isEmpty()) { continue; }
                return headerValues.get(0);
            }
        }
        return null;
    }

    public static boolean containsUpgradeToWebSocketHeader(final Map<String, List<String>> headers) {
        return containsHeaderValue("upgrade", "websocket", headers);
    }

    protected String _url;

    protected HttpMethod _method = HttpMethod.GET;
    protected final List<String> _cookies = new LinkedList<String>();
    protected final Map<String, String> _headers = new HashMap<String, String>();

    protected ByteArray _postData = new MutableByteArray(0);
    protected String _queryString = "";

    protected Boolean _followsRedirects = false;
    protected Integer _maxRedirectCount = 10;
    protected Boolean _validateSslCertificates = true;

    protected Boolean _allowWebSocketUpgrade = false;
    protected WebSocketFactory _webSocketFactory = new DefaultWebSocketFactory();

    protected HttpRequestExecutionThread _executionThread;

    public HttpRequest() { }

    public void setUrl(final String url) {
        _url = url;
    }

    public String getUrl() { return _url; }

    public void setCookie(final String cookie) {
        if (cookie.contains(";")) {
            _cookies.add(cookie.substring(0, cookie.indexOf(";")));
        }
        else {
            _cookies.add(cookie);
        }
    }

    public void setHeader(final String key, final String value) {
        _headers.put(key, value);
    }

    public void setFollowsRedirects(final Boolean followsRedirects) {
        _followsRedirects = followsRedirects;
    }

    public Boolean followsRedirects() {
        return _followsRedirects;
    }

    public void setMethod(final HttpMethod method) {
        _method = method;
    }

    public HttpMethod getMethod() {
        return _method;
    }

    public void setQueryString(final String queryString) {
        _queryString = Util.coalesce(queryString);
    }

    public void setRequestData(final ByteArray byteArray) {
        if (byteArray != null) {
            _postData = byteArray.asConst();
        }
        else {
            _postData = new MutableByteArray(0);
        }
    }

    public void setRequestData(final String requestData) {
        if (requestData != null) {
            _postData = MutableByteArray.wrap(StringUtil.stringToBytes(requestData));
        }
        else {
            _postData = new MutableByteArray(0);
        }
    }

    public void setAllowWebSocketUpgrade(final boolean allowWebSocketUpgrade) {
        _allowWebSocketUpgrade = allowWebSocketUpgrade;
    }

    public void setWebSocketFactory(final WebSocketFactory webSocketFactory) {
        _webSocketFactory = webSocketFactory;
    }

    public boolean allowsWebSocketUpgrade() {
        return _allowWebSocketUpgrade;
    }

    public void setMaxRedirectCount(final int maxRedirectCount) {
        _maxRedirectCount = maxRedirectCount;
    }

    public int getMaxRedirectCount() {
        return _maxRedirectCount;
    }

    public void setValidateSslCertificates(final boolean validateSslCertificates) {
        _validateSslCertificates = validateSslCertificates;
    }

    public boolean validatesSslCertificates() {
        return _validateSslCertificates;
    }

    public HttpResponse execute() {
        final Container<HttpResponse> responseContainer = new Container<HttpResponse>(null);

        _executionThread = new HttpRequestExecutionThread(_url, this, new Callback() {
            @Override
            public void run(final HttpResponse response) {
                responseContainer.value = response;

                _executionThread = null;
            }
        }, 0);
        _executionThread.run();

        return responseContainer.value;
    }

    public void execute(final Callback callback) {
        _executionThread = new HttpRequestExecutionThread(_url, this, callback, 0);
        _executionThread.start();
    }

    public void cancel() {
        final HttpRequestExecutionThread executionThread = _executionThread;
        if (executionThread != null) {
            executionThread.cancel();
        }
    }

    public boolean isExecuting() {
        final HttpRequestExecutionThread executionThread = _executionThread;
        if (executionThread == null) { return false; }

        return executionThread.isExecuting();
    }

    @SuppressWarnings("unused")
    protected static final Class<?>[] unused = {
            HttpsURLConnection.class
    };
}