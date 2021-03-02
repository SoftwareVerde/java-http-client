package org.eclipse.jetty.util.log;

public class Logger {
    public void debug(final Object object) { com.softwareverde.logging.Logger.debug(object); }
    public void warn(final Object object) { com.softwareverde.logging.Logger.warn(object); }
    public void ignore(final Object object) { com.softwareverde.logging.Logger.debug(object); }
}
