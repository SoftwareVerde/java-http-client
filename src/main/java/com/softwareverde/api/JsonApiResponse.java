package com.softwareverde.api;

import com.softwareverde.http.HttpResponse;
import com.softwareverde.json.Json;
import com.softwareverde.util.Util;

public class JsonApiResponse extends ApiResponse {
    private final Json _json;

    private final boolean _wasSuccess;
    private final String _errorMessage;

    public JsonApiResponse(final HttpResponse httpResponse) {
        super(httpResponse);

        _json = httpResponse.getJsonResult();
        _wasSuccess = _json != null && Util.coalesce(_json.getOrNull("wasSuccess", Json.Types.BOOLEAN), true);
        _errorMessage = _json == null ? null : _json.getString("errorMessage");
    }

    public Json getJson() {
        return _json;
    }

    public String getErrorMessage() {
        return _errorMessage;
    }

    @Override
    public boolean isSuccess() {
        return super.isSuccess() && _wasSuccess;
    }
}
