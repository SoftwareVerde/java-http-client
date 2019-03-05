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
//      Removed all methods except for StringUtil::getBytes.
//

package org.eclipse.jetty.util;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.nio.charset.Charset;

/** Fast String Utilities.
 *
 * These string utilities provide both conveniance methods and
 * performance improvements over most standard library versions. The
 * main aim of the optimizations is to avoid object creation unless
 * absolutely required.
 *
 *
 */
public class StringUtil
{
    private static final Logger LOG = Log.getLogger(StringUtil.class);

    public static final String __ISO_8859_1="ISO-8859-1";
    public final static String __UTF8="UTF-8";

    public final static Charset __UTF8_CHARSET;
    public final static Charset __ISO_8859_1_CHARSET;

    static
    {
        __UTF8_CHARSET=Charset.forName(__UTF8);
        __ISO_8859_1_CHARSET=Charset.forName(__ISO_8859_1);
    }

    public static byte[] getBytes(String s)
    {
        try
        {
            return s.getBytes(__ISO_8859_1);
        }
        catch(Exception e)
        {
            LOG.warn(e);
            return s.getBytes();
        }
    }
}