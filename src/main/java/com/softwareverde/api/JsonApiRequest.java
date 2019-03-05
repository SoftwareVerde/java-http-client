package com.softwareverde.api;

import com.softwareverde.json.Json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public abstract class JsonApiRequest extends ApiRequest {
    private final Logger _logger = LoggerFactory.getLogger(getClass());

    public JsonApiRequest() {
        // header added in constructor to allow for it to be overridden afterward if necessary
        this.putHeader("Content-Type", "application/json");
    }

    protected abstract Json _toJson() throws Exception;

    @Override
    public byte[] toBytes() throws Exception {
        final Json jsonObject = _toJson();
        final String jsonString = jsonObject.toString();
        final byte[] jsonBytes = jsonString.getBytes(Charset.forName("UTF-8"));

        _logger.debug("%s%s", "Json message: ", jsonString);

        return jsonBytes;
    }
}
