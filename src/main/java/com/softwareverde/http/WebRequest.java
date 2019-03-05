package com.softwareverde.http;

import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.http.querystring.QueryStringParser;
import com.softwareverde.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebRequest extends HttpRequest {
    protected Map<String, String> _getParams;
    protected Map<String, String> _postParams;
    protected Map<String, List<String>> _arrayGetParams;
    protected Map<String, List<String>> _arrayPostParams;

    protected void _preExecute() {
        if (_queryString.isEmpty()) {
            _queryString = QueryStringParser.toString(_getParams, _arrayGetParams);
        }

        if (_method == HttpMethod.POST) {
            if (_postData.isEmpty()) {
                _postData = MutableByteArray.wrap(StringUtil.stringToBytes(QueryStringParser.toString(_postParams, _arrayPostParams)));
            }

            _headers.put("Content-Type", "application/x-www-form-urlencoded");
            _headers.put("Charset", "UTF-8");
            _headers.put("Content-Length", Integer.toString(_postData.getByteCount()));
        }
    }

    public WebRequest() {
        _getParams = new HashMap<String, String>();
        _postParams = new HashMap<String, String>();

        _arrayGetParams = new HashMap<String, List<String>>();
        _arrayPostParams = new HashMap<String, List<String>>();
    }

    public void setGetParam(String key, String value) {
        if (key == null) { return; }

        if (value == null) {
            _getParams.remove(key);
        }
        else {
            _getParams.put(key, value);
        }
    }

    public void setPostParam(String key, String value) {
        if (key == null) { return; }

        if (value == null) {
            _postParams.remove(key);
        }
        else {
            _postParams.put(key, value);
        }
    }

    public void addGetParam(String key, String value) {
        if (! _arrayGetParams.containsKey(key)) {
            _arrayGetParams.put(key, new ArrayList<String>());
        }

        _getParams.remove(key);

        final List<String> array = _arrayGetParams.get(key);
        array.add(value);
    }

    public void addPostParam(String key, String value) {
        if (! _arrayPostParams.containsKey(key)) {
            _arrayPostParams.put(key, new ArrayList<String>());
        }

        final List<String> array = _arrayPostParams.get(key);
        array.add(value);
    }

    public Map<String, String> getGetParams() {
        final Map<String, String> getParams = new HashMap<String, String>();
        for (final String key : _getParams.keySet()) {
            final String value = _getParams.get(key);
            getParams.put(key, value);
        }
        return getParams;
    }

    public Map<String, String> getPostParams() {
        final Map<String, String> postParams = new HashMap<String, String>();
        for (final String key : _postParams.keySet()) {
            final String value = _postParams.get(key);
            postParams.put(key, value);
        }
        return postParams;
    }

    @Override
    public HttpResponse execute() {
        _preExecute();
        return super.execute();
    }

    @Override
    public void execute(final Callback callback) {
        _preExecute();
        super.execute(callback);
    }
}
