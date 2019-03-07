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
//      Extracted and modified from WebSocketConnectionRFC6455.
//

package org.eclipse.jetty.websocket;

import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.Utf8Appendable;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class WSFrameHandler implements WebSocketParser.FrameHandler {
    public interface Callback<T> {
        void onMessage(T message);
    }

    public interface CloseSocketHandler {
        void close(int code, String message);
    }

    protected static final Logger LOG = Log.getLogger(WSFrameHandler.class);

    private static final int MAX_CONTROL_FRAME_PAYLOAD = 125;
    private final Utf8StringBuilder _utf8;
    private ByteArrayBuffer _aggregate;
    private byte _opcode = -1;

    protected Integer _maxMessageSize;
    final protected CloseSocketHandler _closeSocketHandler;
    protected Callback<String> _textMessageCallback;
    protected Callback<byte[]> _binaryMessageCallback;
    protected Callback<byte[]> _pingMessageCallback;
    protected Callback<byte[]> _pongMessageCallback;


    private void _errorClose(final int code, final String message) {
        _closeSocketHandler.close(code, message);
    }

    private boolean _checkBinaryMessageSize(final int bufferLen, final int length) {
        if ( (_maxMessageSize > 0) && ((bufferLen + length) > _maxMessageSize) ) {
            LOG.warn("Binary message too large.");
            _closeSocketHandler.close(WebSocketConnectionRFC6455.CLOSE_MESSAGE_TOO_LARGE, "");
            _opcode = -1;
            if (_aggregate != null) {
                _aggregate.clear();
            }
            return false;
        }
        return true;
    }

    private void _textMessageTooLarge() {
        LOG.warn("Text message too large.");
        _closeSocketHandler.close(WebSocketConnectionRFC6455.CLOSE_MESSAGE_TOO_LARGE, "");

        _opcode = -1;
        _utf8.reset();
    }

    public WSFrameHandler(final Integer maxMessageSize, final CloseSocketHandler closeSocketHandler) {
        _maxMessageSize = maxMessageSize;
        _utf8 = new Utf8StringBuilder(maxMessageSize);
        _closeSocketHandler = closeSocketHandler;
    }

    public void setTextMessageCallback(final Callback<String> textMessageCallback) {
        _textMessageCallback = textMessageCallback;
    }

    public void setBinaryMessageCallback(final Callback<byte[]> binaryMessageCallback) {
        _binaryMessageCallback = binaryMessageCallback;
    }

    public void setPingMessageCallback(final Callback<byte[]> pingMessageCallback) {
        _pingMessageCallback = pingMessageCallback;
    }

    public void setPongMessageCallback(final Callback<byte[]> pongMessageCallback) {
        _pongMessageCallback = pongMessageCallback;
    }

    @Override
    public void onFrame(final byte flags, final byte opcode, final Buffer buffer) {
        final boolean isLastFrame = WebSocketConnectionRFC6455.isLastFrame(flags);

        try {
            // final byte[] array = buffer.array();

            if (WebSocketConnectionRFC6455.isControlFrame(opcode) && buffer.length()>MAX_CONTROL_FRAME_PAYLOAD) {
                _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL,"Control frame too large: " + buffer.length() + " > " + MAX_CONTROL_FRAME_PAYLOAD);
                return;
            }

            if ((flags & 0x07) != 0) {
                _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL,"RSV bits set 0x"+Integer.toHexString(flags));
                return;
            }

            switch (opcode) {
                case WebSocketConnectionRFC6455.OP_CONTINUATION: {

                    switch (_opcode) {
                        case WebSocketConnectionRFC6455.OP_TEXT: {
                            if (isLastFrame) { _opcode = -1; }

                            final Callback<String> onTextMessage = _textMessageCallback;
                            if (onTextMessage != null) {
                                if (_utf8.append(buffer.array(), buffer.getIndex(), buffer.length(), _maxMessageSize)) {
                                    if (isLastFrame) {
                                        final String msg = _utf8.toString();
                                        _utf8.reset();
                                        onTextMessage.onMessage(msg);
                                    }
                                }
                                else {  
                                    _textMessageTooLarge();
                                }
                            }
                        } break;

                        case WebSocketConnectionRFC6455.OP_BINARY: {
                            if (isLastFrame) { _opcode = -1; }

                            final Callback<byte[]> onBinaryMessage = _binaryMessageCallback;
                            if (onBinaryMessage != null) {
                                if ( (_aggregate != null) && (_checkBinaryMessageSize(_aggregate.length(), buffer.length())) ) {
                                    _aggregate.put(buffer);

                                    if (isLastFrame) {
                                        try {
                                            onBinaryMessage.onMessage(_aggregate.asArray());
                                        }
                                        finally {
                                            _aggregate.clear();
                                        }
                                    }
                                }
                            }
                        } break;

                        default: {
                            _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL, "Bad Continuation");
                            return;
                        }
                    }
                    break;
                }

                case WebSocketConnectionRFC6455.OP_PING: {
                    final Callback<byte[]> onPingMessage = _pingMessageCallback;
                    if (onPingMessage != null) {
                        onPingMessage.onMessage(buffer.asArray());
                    }
                } break;

                case WebSocketConnectionRFC6455.OP_PONG: {
                    final Callback<byte[]> onPongMessage = _pongMessageCallback;
                    if (onPongMessage != null) {
                        onPongMessage.onMessage(buffer.asArray());
                    }
                } break;

                case WebSocketConnectionRFC6455.OP_CLOSE: {
                    int code = WebSocketConnectionRFC6455.CLOSE_NO_CODE;
                    String message = null;
                    if (buffer.length() >= 2) {
                        final byte[] bufferBytes = buffer.array();
                        code = (0xFF & bufferBytes[buffer.getIndex()]) * 0x100 + (0xFF & bufferBytes[buffer.getIndex() + 1]);

                        boolean isValidClose = true;
                        if (code < WebSocketConnectionRFC6455.CLOSE_NORMAL) { isValidClose = false; }
                        else if (code == WebSocketConnectionRFC6455.CLOSE_UNDEFINED) { isValidClose = false; }
                        else if (code == WebSocketConnectionRFC6455.CLOSE_NO_CLOSE) { isValidClose = false; }
                        else if (code == WebSocketConnectionRFC6455.CLOSE_NO_CODE) { isValidClose = false; }
                        else if ( (code > 1011 && code <= 2999 ) ) { isValidClose = false; }
                        else if (code >= 5000) { isValidClose = false; }

                        if (! isValidClose) {
                            _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL,"Invalid close code " + code);
                            return;
                        }

                        if (buffer.length() > 2) {
                            if (_utf8.append(buffer.array(),buffer.getIndex() + 2,buffer.length() - 2, _maxMessageSize)) {
                                message = _utf8.toString();
                                _utf8.reset();
                            }
                        }
                    }
                    else if (buffer.length() == 1) {
                        // Invalid length. use status code 1002 (Protocol error)
                        _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL,"Invalid payload length of 1");
                        return;
                    }

                    _closeSocketHandler.close(code, message);
                } break;

                case WebSocketConnectionRFC6455.OP_TEXT: {
                    if (_opcode != -1) {
                        _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL,"Expected Continuation" + Integer.toHexString(opcode));
                        return;
                    }

                    _opcode = (isLastFrame ? -1 : WebSocketConnectionRFC6455.OP_TEXT);

                    final Callback<String> onTextMessage = _textMessageCallback;
                    if (onTextMessage != null) {
                        if (_maxMessageSize <= 0) {
                            // No size limit, so handle only final frames
                            if (isLastFrame) {
                                onTextMessage.onMessage(buffer.toString(StringUtil.__UTF8));
                            }
                            else {
                                LOG.warn("Frame discarded. Text aggregation disabled.");
                                _errorClose(WebSocketConnectionRFC6455.CLOSE_POLICY_VIOLATION,"Text frame aggregation disabled");
                            }
                        }
                        else if (_utf8.append(buffer.array(), buffer.getIndex(), buffer.length(), _maxMessageSize)) {
                            if (isLastFrame) {
                                final String msg =_utf8.toString();
                                _utf8.reset();
                                onTextMessage.onMessage(msg);
                            }
                        }
                        else
                            _textMessageTooLarge();
                    }
                } break;

                case WebSocketConnectionRFC6455.OP_BINARY: {
                    if (_opcode != -1) {
                        _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL,"Expected Continuation"+Integer.toHexString(opcode));
                        return;
                    }

                    _opcode = (isLastFrame ? -1 : WebSocketConnectionRFC6455.OP_BINARY);

                    final Callback<byte[]> onBinaryMessage = _binaryMessageCallback;
                    if (onBinaryMessage != null) {
                        if(! _checkBinaryMessageSize(0, buffer.length())) { return; }

                        if (isLastFrame) {
                            final byte[] bytes = buffer.asArray();
                            onBinaryMessage.onMessage(bytes);
                        }
                        else if (_maxMessageSize >= 0) {
                            if (_aggregate == null) {
                                _aggregate = new ByteArrayBuffer(_maxMessageSize);
                                _aggregate.clear();
                            }

                            _aggregate.put(buffer);
                        }
                        else {
                            LOG.warn("Frame discarded. Binary aggregation disabled.");
                            _errorClose(WebSocketConnectionRFC6455.CLOSE_POLICY_VIOLATION,"Binary frame aggregation disabled");
                        }
                    }
                } break;

                default: {
                    _errorClose(WebSocketConnectionRFC6455.CLOSE_PROTOCOL, "Bad opcode 0x" + Integer.toHexString(opcode));
                } break;
            }
        }
        catch (final Utf8Appendable.NotUtf8Exception notUtf8) {
            _errorClose(WebSocketConnectionRFC6455.CLOSE_BAD_PAYLOAD,"Invalid UTF-8");
        }
        catch (final Exception exception) {
            _errorClose(WebSocketConnectionRFC6455.CLOSE_SERVER_ERROR,"Internal Server Error.");
        }
    }

    @Override
    public void close(final int code, final String message) {
        _closeSocketHandler.close(code, message);
    }
}