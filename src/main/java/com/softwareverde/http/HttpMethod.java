package com.softwareverde.http;

public enum HttpMethod {
    GET, POST, HEAD, PATCH, PUT, DELETE, OPTIONS, TRACE;

    public static HttpMethod fromString(final String string) {
        for (final HttpMethod httpMethod : HttpMethod.values()) {
            if (httpMethod.name().equalsIgnoreCase(string)) {
                return httpMethod;
            }
        }

        return null;
    }
}