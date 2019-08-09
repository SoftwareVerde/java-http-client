package com.softwareverde.api;

import com.softwareverde.json.Json;
import com.softwareverde.logging.Logger;
import com.softwareverde.logging.LoggerInstance;

import java.nio.charset.Charset;

public abstract class JsonApiRequest extends ApiRequest {
    protected final LoggerInstance _logger = Logger.getInstance(JsonApiRequest.class);

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

        _logger.debug("Json message: " + jsonString);

        return jsonBytes;
    }
}
