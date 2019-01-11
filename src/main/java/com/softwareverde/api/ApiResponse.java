package com.softwareverde.api;

import com.softwareverde.http.HttpResponse;

public class ApiResponse {
    private final int _httpResponseCode;
    private final String _httpResponseMessage;

    public ApiResponse(final HttpResponse httpResponse) {
        _httpResponseCode = httpResponse.getHttpResponseCode();
        _httpResponseMessage = httpResponse.getHttpResponseMessage();
    }

    /**
     * Returns the HTTP response code.
     * @return
     */
    public int getHttpResponseCode() {
        return _httpResponseCode;
    }

    /**
     * Returns the HTTP response message (e.g. "OK").  Does not include the response code.
     * @return
     */
    public String getHttpResponseMessage() {
        return _httpResponseMessage;
    }

    /**
     * Returns a string indicating the HTTP response code and message (e.g. "200 OK").
     * @return
     */
    @Override
    public String toString() {
        return _httpResponseCode + " " + _httpResponseMessage;
    }

    public boolean isSuccess() {
        return (_httpResponseCode >= 200 && _httpResponseCode < 300);
    }
}
