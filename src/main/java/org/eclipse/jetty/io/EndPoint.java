//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//
// Modifications:
//  2019 - Software Verde, LLC
//      Removed unused methods.
//  2020 - Software Verde, LLC
//      Removed unused methods.
//

package org.eclipse.jetty.io;

import java.io.IOException;


/**
 *
 * A transport EndPoint
 */
public interface EndPoint
{
    /**
     * Shutdown any backing output stream associated with the endpoint
     */
    void shutdownOutput() throws IOException;


    boolean isInputShutdown();

    /**
     * Fill the buffer from the current putIndex to it's capacity from whatever
     * byte source is backing the buffer. The putIndex is increased if bytes filled.
     * The buffer may chose to do a compact before filling.
     * @return an <code>int</code> value indicating the number of bytes
     * filled or -1 if EOF is reached.
     * @throws EofException If input is shutdown or the endpoint is closed.
     */
    int fill(Buffer buffer) throws IOException;

    /**
     * Flush the buffer from the current getIndex to it's putIndex using whatever byte
     * sink is backing the buffer. The getIndex is updated with the number of bytes flushed.
     * Any mark set is cleared.
     * If the entire contents of the buffer are flushed, then an implicit empty() is done.
     *
     * @param buffer The buffer to flush. This buffers getIndex is updated.
     * @return  the number of bytes written
     * @throws EofException If the endpoint is closed or output is shutdown.
     */
    int flush(Buffer buffer) throws IOException;

    /* ------------------------------------------------------------ */
    public boolean isOpen();

}