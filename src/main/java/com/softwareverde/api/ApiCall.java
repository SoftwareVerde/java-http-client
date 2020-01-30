package com.softwareverde.api;

import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.http.HttpRequest;
import com.softwareverde.http.HttpResponse;
import com.softwareverde.logging.Logger;
import com.softwareverde.logging.LoggerInstance;
import com.softwareverde.util.Util;

public abstract class ApiCall<REQUEST extends ApiRequest, RESPONSE extends ApiResponse> {
    private final LoggerInstance _logger = Logger.getInstance(getClass());

    private final ApiConfiguration _configuration;

    public ApiCall(final ApiConfiguration configuration) {
        _configuration = configuration;
    }

    protected ApiConfiguration _getConfiguration() {
        return _configuration;
    }

    public abstract RESPONSE call(final REQUEST request) throws Exception;

    protected HttpResponse _call(final String requestPath, final HttpMethod requestMethod, final REQUEST request) throws Exception {
        long startTime = System.currentTimeMillis();
        final HttpRequest httpRequest;
        try {
            final String baseUrl = _getConfiguration().getApiUrl();
            final String fullUrl = baseUrl + Util.coalesce(requestPath);

            final ByteArray requestData = MutableByteArray.wrap(request.toBytes());

            httpRequest = new HttpRequest();
            httpRequest.setUrl(fullUrl);
            httpRequest.setMethod(requestMethod);
            httpRequest.setRequestData(requestData);
            for (final String header : request.getHeaderNames()) {
                final String value = request.getHeader(header);
                httpRequest.setHeader(header, value);
            }
        }
        catch (final Exception exception) {
            throw new RuntimeException("Unable to build HTTP request for " + request.getClass().getSimpleName(), exception);
        }

        HttpResponse httpResponse = null;
        try {
            httpResponse = httpRequest.execute();
            return httpResponse;
        }
        finally {
            if (httpResponse != null) {
                long duration = System.currentTimeMillis() - startTime;
                final String httpResponseString = httpResponse.getResponseCode() + " " + httpResponse.getResponseMessage();
                _logger.info(requestMethod.name() + " | " + httpRequest.getUrl() + " | " + httpResponseString + " | " + duration + " ms");
            }
        }
    }

}
