package com.softwareverde.http.cookie;

import com.softwareverde.util.Util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Cookie Class as defined by: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie
 */
public class Cookie {
    protected static final DateFormat _dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    static {
        _dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Returns the expirationDateString as a Time with ms.
     *  If the expirationDateString is in an invalid format, null is returned.
     */
    public static Long parseExpirationDate(final String expirationDateString) {
        try {
            return _dateFormat.parse(expirationDateString).getTime();
        }
        catch (final ParseException exception) {
            return null;
        }
    }

    public static String formatExpirationDate(final Long expirationTime) {
        return _dateFormat.format(new Date(expirationTime));
    }

    public static String sanitizeValue(final String value) {
        // Strip CTLs, whitespace, double quotes, comma, semicolon, and backslash...
        return value.replaceAll("[\\x00-\\x1F\\x7F \t\",;\\\\]", "");
    }

    protected String _key;
    protected String _value;
    protected String _expirationDate;
    protected Integer _maxAge;
    protected String _domain;
    protected String _path;
    protected Boolean _isSecure = true;
    protected Boolean _isHttpOnly = true;
    protected Boolean _isSameSiteStrict = true;

    protected void _setExpirationDateFromMaxAge() {
        if (_maxAge == null) {
            _expirationDate = null;
            return;
        }

        final Long now = System.currentTimeMillis();
        _expirationDate = Cookie.formatExpirationDate(now + (_maxAge * 1000L));
    }

    protected void _setMaxAgeFromExpirationDate() {
        if (_expirationDate == null) {
            _maxAge = null;
            return;
        }

        final Long now = System.currentTimeMillis();
        final Long expirationTime = Cookie.parseExpirationDate(_expirationDate);
        if (expirationTime == null) {
            _maxAge = null;
        }
        else {
            _maxAge = (int) ((expirationTime - now) / 1000L);
        }
    }

    public Cookie() { }
    public Cookie(final String key, final String value) {
        _key = key;
        _value = value;
    }

    public String getKey() { return _key; }
    public String getValue() { return _value; }
    public String getExpirationDate() { return _expirationDate; }
    public Integer getMaxAge() { return _maxAge; }
    public String getDomain() { return _domain; }
    public String getPath() { return _path; }
    public Boolean isSecure() { return _isSecure; }
    public Boolean isHttpOnly() { return _isHttpOnly; }
    public Boolean isSameSiteStrict() { return _isSameSiteStrict; }

    public void setKey(final String key) { _key = key; }
    public void setValue(final String value) { _value = Cookie.sanitizeValue(value); }

    /**
     * Sets the Expires property of the Cookie.
     *  May be overwritten by subsequent calls to setMaxAge().
     *  If the maxAge has been set, maxAge will be updated to represent the expirationDate.
     *      maxAge will be set as if the cookie was issued immediately.
     *      Therefore, if the Cookie is issued later, the maxAge will be later than the expirationDate.
     */
    public void setExpirationDate(final String expirationDate) {
        _expirationDate = expirationDate;

        if (_maxAge != null && _expirationDate != null) {
            _setMaxAgeFromExpirationDate();
        }
    }
    public void setExpirationDate(final Long expirationDate) {
        if (expirationDate == null) {
            _expirationDate = null;
        }
        else {
            _expirationDate = Cookie.formatExpirationDate(expirationDate);
        }

        if (_maxAge != null && _expirationDate != null) {
            _setMaxAgeFromExpirationDate();
        }
    }

    /**
     * Sets the Max-Age property of the Cookie.
     *  May be overwritten to subsequent calls to setExpirationDate().
     *  If the expirationDate has been set, expirationDate will be update to represent maxAge.
     *      expirationDate will be set as if the cookie was issued immediately.
     *      Therefore, if the cookie is issued later, the expirationDate will be before the maxAge.
     *      Note: Most modern browsers will disregard the Expires segment if the Max-Age segment is present;
     *          Therefore, it if setting both maxAge and expirationDate, it is recommended to set expirationDate first.
     */
    public void setMaxAge(final Integer maxAge) {
        _maxAge = maxAge;

        if (_expirationDate != null && _maxAge != null) {
            _setExpirationDateFromMaxAge();
        }
    }

    /**
     * Sets the Max-Age property of the Cookie.
     *  If setExpirationDate is true, then the Expires property will be set, regardless if it has/hasn't been set.
     *  Setting setExpirationDate to false will not update the Expires property, even if it is mismatched.
     */
    public void setMaxAge(final Integer maxAge, final Boolean setExpirationDate) {
        _maxAge = maxAge;

        if (setExpirationDate) {
            _setExpirationDateFromMaxAge();
        }
    }

    public void setDomain(final String domain) { _domain = domain; }
    public void setPath(final String path) { _path = path; }
    public void setIsSecure(final Boolean isSecure) { _isSecure = isSecure; }
    public void setIsHttpOnly(final Boolean isHttpOnly) { _isHttpOnly = isHttpOnly; }
    public void setIsSameSiteStrict(final Boolean isSameSiteStrict) { _isSameSiteStrict = isSameSiteStrict; }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(_key);
        stringBuilder.append("=");
        stringBuilder.append(_value);
        stringBuilder.append("; ");

        if (_expirationDate != null) {
            stringBuilder.append("Expires=");
            stringBuilder.append(_expirationDate);
            stringBuilder.append("; ");
        }

        if (_maxAge != null) {
            stringBuilder.append("Max-Age=");
            stringBuilder.append(_maxAge);
            stringBuilder.append("; ");
        }

        if (_domain != null) {
            stringBuilder.append("Domain=");
            stringBuilder.append(_domain);
            stringBuilder.append("; ");
        }

        if (_path != null) {
            stringBuilder.append("Path=");
            stringBuilder.append(_path);
            stringBuilder.append("; ");
        }

        if (Util.coalesce(_isSecure)) {
            stringBuilder.append("Secure");
            stringBuilder.append("; ");
        }

        if (Util.coalesce(_isHttpOnly)) {
            stringBuilder.append("HttpOnly");
            stringBuilder.append("; ");
        }

        if (_isSameSiteStrict != null) {
            stringBuilder.append("SameSite=");
            stringBuilder.append(_isSameSiteStrict ? "Strict" : "Lax");
            stringBuilder.append("; ");
        }

        return stringBuilder.toString();
    }
}
