package com.softwareverde.http.querystring;

import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryStringParser<T extends QueryString> {
    public interface QueryStringFactory<T extends QueryString> {
        T newInstance();
    }

    public static String toString(final Map<String, String> params, final Map<String, List<String>> arrayParams) {
        final StringBuilder stringBuilder = new StringBuilder();
        String separator = "";
        try {
            for (String key : params.keySet()) {
                String value = params.get(key);
                stringBuilder.append(separator);
                stringBuilder.append(URLEncoder.encode(key, "UTF-8"));
                stringBuilder.append("=");
                stringBuilder.append(URLEncoder.encode(Util.coalesce(value), "UTF-8"));
                separator = "&";
            }

            for (final String key : arrayParams.keySet()) {
                final List<String> values = arrayParams.get(key);

                for (final String value : values) {
                    stringBuilder.append(separator);
                    stringBuilder.append(URLEncoder.encode(key + "[]", "UTF-8"));
                    stringBuilder.append("=");
                    stringBuilder.append(URLEncoder.encode(Util.coalesce(value), "UTF-8"));
                    separator = "&";
                }
            }
        }
        catch (final Exception exception) {
            exception.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private final QueryStringFactory<T> _queryStringFactory;

    private Boolean _isKeyForAnArray(final String key) {
        if ( (key == null) || (key.length() < 2) ) { return false; }
        return (key.substring(key.length() - 2).equals("[]"));
    }

    private String _transformArrayKey(final String key) {
        return key.substring(0, key.length() - 2);
    }

    public QueryStringParser(final QueryStringFactory<T> queryStringFactory) {
        _queryStringFactory = queryStringFactory;
    }

    public T parse(final String query) {
        final Map<String, QueryStringParameter> parameters = new HashMap<String, QueryStringParameter>();
        if (query != null) {
            final String[] pairs = query.split("[&]");

            for (final String pair : pairs) {
                final String[] keyValuePair = pair.split("[=]");

                String key = null;
                String value = null;
                if (keyValuePair.length == 2) {
                    try {
                        key = URLDecoder.decode(keyValuePair[0], "UTF-8");
                        value = URLDecoder.decode(keyValuePair[1], "UTF-8");
                    }
                    catch (final UnsupportedEncodingException e) { }
                }

                if ( (key != null) && (value != null) ) {
                    if (_isKeyForAnArray(key)) {
                        final String arrayKey = _transformArrayKey(key);

                        final QueryStringParameter parameter;
                        if (parameters.containsKey(arrayKey)) {
                            parameter = parameters.get(arrayKey);
                        }
                        else {
                            parameter = new QueryStringParameter();
                        }

                        parameter.addValue(value);
                        parameters.put(arrayKey, parameter);
                    }
                    else {
                        parameters.put(key, new QueryStringParameter(value));
                    }
                }
            }
        }

        final T queryString = _queryStringFactory.newInstance();
        queryString._setValues(parameters);
        return queryString;
    }
}