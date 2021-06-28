package com.softwareverde.http;

import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.http.websocket.WebSocket;
import com.softwareverde.json.Json;
import com.softwareverde.util.StringUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    protected ByteArray _rawResult;
    protected Integer _responseCode;
    protected String _responseMessage;
    protected WebSocket _webSocket = null;
    protected Map<String, List<String>> _headers = null;

    // NOTE: Handles both android-formatted and ios-formatted cookie strings.
    //  iOS concatenates their cookies into one string, delimited by commas;
    //  Android cookies are separate cookie-records.
    protected List<String> _parseCookies(final String cookie) {
        final List<String> cookies = new LinkedList<String>();

        if (cookie.contains(";")) {
            Boolean skipNext = false;
            for (final String cookieSegment : cookie.replaceAll(",", ";").split(";")) {
                if (skipNext) {
                    skipNext = false;
                    continue;
                }

                final String cleanedCookie = cookieSegment.trim();

                if (cleanedCookie.toLowerCase().contains("expires=")) {
                    skipNext = true;
                    continue;
                }
                if (cleanedCookie.toLowerCase().contains("max-age=")) {
                    continue;
                }
                if (cleanedCookie.toLowerCase().contains("path=")) {
                    continue;
                }
                if (cleanedCookie.toLowerCase().contains("httponly")) {
                    continue;
                }

                cookies.add(cleanedCookie);
            }
        }
        else {
            cookies.add(cookie.trim());
        }

        return cookies;
    }

    public WebSocket getWebSocket() {
        return _webSocket;
    }

    public Integer getResponseCode() { return _responseCode; }

    public String getResponseMessage() { return _responseMessage; }

    public synchronized Json getJsonResult() {
        if (_rawResult == null) {
            return null;
        }
        return Json.parse(StringUtil.bytesToString(_rawResult.getBytes()));
    }

    public synchronized ByteArray getRawResult() { return _rawResult; }

    public Map<String, List<String>> getHeaders() {
        return _headers;
    }

    public List<String> getCookies() {
        if (_headers.containsKey("Set-Cookie")) {
            List<String> cookies = new LinkedList<String>();
            for (final String cookie : _headers.get("Set-Cookie")) {
                cookies.addAll(_parseCookies(cookie));
            }

            return cookies;
        }

        return new LinkedList<String>();
    }

    public boolean didUpgradeToWebSocket() {
        return (_webSocket != null);
    }
}
