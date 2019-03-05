package com.softwareverde.api;

import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.http.HttpRequest;
import com.softwareverde.http.HttpResponse;
import com.softwareverde.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ApiCall<REQUEST extends ApiRequest, RESPONSE extends ApiResponse> {
    private final Logger _logger = LoggerFactory.getLogger(ApiCall.class);

    private final ApiConfiguration _configuration;

    public ApiCall(final ApiConfiguration configuration) {
        _configuration = configuration;
    }

    protected ApiConfiguration _getConfiguration() {
        return _configuration;
    }

    public abstract RESPONSE call(final REQUEST request) throws Exception;

    protected HttpResponse _call(final String requestPath, final HttpMethod requestMethod, final ApiRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String apiUrl = "";
        try {
            apiUrl = _getConfiguration().getApiUrl();

            final ByteArray requestData = MutableByteArray.wrap(request.toBytes());

            final HttpRequest httpRequest = new HttpRequest();
            httpRequest.setUrl(apiUrl + Util.coalesce(requestPath));
            httpRequest.setMethod(requestMethod);
            httpRequest.setRequestData(requestData);
            for (final String header : request.getHeaderNames()) {
                final String value = request.getHeader(header);
                httpRequest.setHeader(header, value);
            }

            final HttpResponse httpResponse = httpRequest.execute();
            _logger.debug("%s%s", "Received ", httpResponse);
            return httpResponse;
        }
        finally {
            long duration = System.currentTimeMillis() - startTime;
            final String url = apiUrl + requestPath;
            _logger.info("Processed " + requestMethod.name() + " request to " + url + " in " + duration + "ms.");
        }
    }

}
