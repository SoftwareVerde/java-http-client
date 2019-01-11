package com.softwareverde.api;

import java.util.HashMap;
import java.util.Set;

public abstract class ApiRequest {
    private final HashMap<String, String> _headers = new HashMap<>();

    public Set<String> getHeaderNames() {
        return _headers.keySet();
    }

    public String getHeader(final String header) {
        return _headers.get(header);
    }

    public void putHeader(final String header, final String value) {
        _headers.put(header, value);
    }

    public abstract byte[] toBytes() throws Exception;
}
