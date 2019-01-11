package com.softwareverde.http;

import com.softwareverde.json.Json;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class HttpResponse {
    private final Logger _logger = LoggerFactory.getLogger(getClass());

    private int _httpResponseCode;
    private String _httpResponseMessage;
    private byte[] _responseData;

    public HttpResponse(final byte[] responseData, final int responseCode, final String responseMessage) {
        _responseData = responseData;
        _httpResponseCode = responseCode;
        _httpResponseMessage = responseMessage;
    }

    public int getHttpResponseCode() {
        return _httpResponseCode;
    }

    public String getHttpResponseMessage() {
        return _httpResponseMessage;
    }

    public byte[] getResponseData() {
        return _responseData;
    }

    public Json getResponseJson() {
        if (_responseData == null) {
            return null;
        }
        final String responseDataString = new String(_responseData, Charset.forName("UTF-8"));

        _logger.debug("%s%s", "Json message: ", responseDataString);

        try {
            final JSONObject jsonObject = new JSONObject(responseDataString);
            final Json json = Json.wrap(jsonObject);
            return json;
        }
        catch (final JSONException jsonException) {
            throw new IllegalStateException("Unable to parse response data as a JSON object, response: " + _httpResponseCode + " " + _httpResponseMessage, jsonException);
        }
    }

    public boolean isSuccess() {
        return (_httpResponseCode >= 200 && _httpResponseCode < 300);
    }

    @Override
    public String toString() {
        return "" + _httpResponseCode + " " + _httpResponseMessage + ": " + new String(_responseData);
    }
}
