package org.eclipse.jetty.util.log;

public class Log {
    public static Logger getLogger(final Object object) {
        return new Logger();
    }
}
