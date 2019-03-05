package com.softwareverde.http.querystring;

import java.util.*;

public class QueryString {
    private Map<String, QueryStringParameter> _values = new HashMap<String, QueryStringParameter>();

    protected QueryString() { }

    protected void _setValues(final Map<String, QueryStringParameter> values) {
        _values = values;
    }

    protected Boolean _isArray(final String key) {
        if (! _values.containsKey(key)) { return false; }
        return _values.get(key).isArray();
    }

    public String _get(final String key) {
        if (! _values.containsKey(key)) { return ""; }

        final QueryStringParameter queryStringParameter = _values.get(key);
        if (queryStringParameter.isArray()) {
            return queryStringParameter.getValues().get(0);
        }
        else {
            return queryStringParameter.getValue();
        }
    }

    protected String[] _getArray(final String key) {
        if (! _values.containsKey(key)) { return new String[0]; }

        final QueryStringParameter queryStringParameter = _values.get(key);
        if (queryStringParameter.isArray()) {
            return queryStringParameter.getValues().toArray(new String[0]);
        }
        else {
            return new String[]{ queryStringParameter.getValue() };
        }
    }

    public Set<String> getKeys() {
        return _values.keySet();
    }

    /**
     * Returns true if the key exists within the QueryString and is an array.
     *  Returns false if the key does not exist within the QueryString.
     */
    public Boolean isArray(final String key) {
        return _isArray(key);
    }

    /**
     * Returns true if the key exists in the QueryString.
     *  NOTE: If the key exists, this function returns true regardless if the key is associated with a value or array.
     */
    public Boolean containsKey(final String key) { return _values.containsKey(key); }

    /**
     * Returns the value associated with the provided key.
     *  If the key does not exist, an empty string is returned.
     *  If the key is associated with an array, the first value of the array is returned.
     */
    public String get(final String key) {
        return _get(key);
    }

    /**
     * Returns the value(s) associated with the provided key.
     *  If the key does not exist, an empty array is returned.
     *  If the key is associated with a value, an array containing that value is turned.
     */
    public String[] getArray(final String key) {
        return _getArray(key);
    }

    @Override
    public String toString() {
        final HashMap<String, String> params = new HashMap<String, String>();
        final HashMap<String, List<String>> arrayParams = new HashMap<String, List<String>>();

        for (final String key : _values.keySet()) {
            if (_isArray(key)) {
                arrayParams.put(key, Arrays.asList(_getArray(key)));
            }
            else {
                params.put(key, _get(key));
            }
        }

        return QueryStringParser.toString(params, arrayParams);
    }
}