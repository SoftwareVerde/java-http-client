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
//      Removed all non-static methods.
//

package org.eclipse.jetty.websocket;

/* ------------------------------------------------------------ */
/**
 * <pre>
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-------+-+-------------+-------------------------------+
 *   |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 *   |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
 *   |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 *   | |1|2|3|       |K|             |                               |
 *   +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 *   |     Extended payload length continued, if payload len == 127  |
 *   + - - - - - - - - - - - - - - - +-------------------------------+
 *   |                               |Masking-key, if MASK set to 1  |
 *   +-------------------------------+-------------------------------+
 *   | Masking-key (continued)       |          Payload Data         |
 *   +-------------------------------- - - - - - - - - - - - - - - - +
 *   :                     Payload Data continued ...                :
 *   + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 *   |                     Payload Data continued ...                |
 *   +---------------------------------------------------------------+
 * </pre>
 */
public class WebSocketConnectionRFC6455
{
    public static final byte OP_CONTINUATION = 0x00;
    public static final byte OP_TEXT = 0x01;
    public static final byte OP_BINARY = 0x02;
    public static final byte OP_EXT_DATA = 0x03;

    public static final byte OP_CONTROL = 0x08;
    public static final byte OP_CLOSE = 0x08;
    public static final byte OP_PING = 0x09;
    public static final byte OP_PONG = 0x0A;
    public static final byte OP_EXT_CTRL = 0x0B;

    public static final int CLOSE_NORMAL=1000;
    public static final int CLOSE_SHUTDOWN=1001;
    public static final int CLOSE_PROTOCOL=1002;
    public static final int CLOSE_BAD_DATA=1003;
    public static final int CLOSE_UNDEFINED=1004;
    public static final int CLOSE_NO_CODE=1005;
    public static final int CLOSE_NO_CLOSE=1006;
    public static final int CLOSE_BAD_PAYLOAD=1007;
    public static final int CLOSE_POLICY_VIOLATION=1008;
    public static final int CLOSE_MESSAGE_TOO_LARGE=1009;
    public static final int CLOSE_REQUIRED_EXTENSION=1010;
    public static final int CLOSE_SERVER_ERROR=1011;
    public static final int CLOSE_FAILED_TLS_HANDSHAKE=1015;

    public static final int FLAG_FIN=0x8;

    // Per RFC 6455, section 1.3 - Opening Handshake - this version is "13"
    public static final int VERSION=13;

    static boolean isLastFrame(byte flags)
    {
        return (flags&FLAG_FIN)!=0;
    }

    static boolean isControlFrame(byte opcode)
    {
        return (opcode&OP_CONTROL)!=0;
    }
}