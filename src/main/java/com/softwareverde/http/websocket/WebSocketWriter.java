package com.softwareverde.http.websocket;

import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.websocket.MaskGen;
import org.eclipse.jetty.websocket.WebSocketBuffers;
import org.eclipse.jetty.websocket.WebSocketConnectionRFC6455;
import org.eclipse.jetty.websocket.WebSocketGeneratorRFC6455;

import java.io.IOException;
import java.security.SecureRandom;

class WebSocketWriter {
    protected final SocketStreams _endPoint;
    protected final WebSocketGeneratorRFC6455 _webSocketGeneratorRFC6455;

    public WebSocketWriter(final WebSocket.Mode mode, final WebSocketBuffers webSocketBuffers, final SocketStreams endPoint) {
        final MaskGen maskGen;
        if (mode == WebSocket.Mode.SERVER) {
            maskGen = null;
        }
        else {
            maskGen = new MaskGen() {
                private final SecureRandom _secureRandom = new SecureRandom();

                @Override
                public void genMask(final byte[] mask) {
                    _secureRandom.nextBytes(mask);
                }
            };
        }

        _webSocketGeneratorRFC6455 = new WebSocketGeneratorRFC6455(webSocketBuffers, endPoint, maskGen);
        _endPoint = endPoint;
    }

    public void writeMessage(final String message) {
        final byte[] bytes = StringUtil.getBytes(message);
        try {
            _webSocketGeneratorRFC6455.addFrame((byte) WebSocketConnectionRFC6455.FLAG_FIN, WebSocketConnectionRFC6455.OP_TEXT, bytes, 0, bytes.length);
        }
        catch (final IOException exception) {
            _endPoint.shutdown();
        }
    }

    public void writeMessage(final byte[] bytes) {
        try {
            _webSocketGeneratorRFC6455.addFrame((byte) WebSocketConnectionRFC6455.FLAG_FIN, WebSocketConnectionRFC6455.OP_BINARY, bytes, 0, bytes.length);
        }
        catch (final IOException exception) {
            _endPoint.shutdown();
        }
    }

    public void writePong(final byte[] bytes) {
        try {
            _webSocketGeneratorRFC6455.addFrame((byte) WebSocketConnectionRFC6455.FLAG_FIN, WebSocketConnectionRFC6455.OP_PONG, bytes, 0, bytes.length);
        }
        catch (final IOException exception) {
            _endPoint.shutdown();
        }
    }

    public void writePing(final byte[] bytes) {
        try {
            _webSocketGeneratorRFC6455.addFrame((byte) WebSocketConnectionRFC6455.FLAG_FIN, WebSocketConnectionRFC6455.OP_PING, bytes, 0, bytes.length);
        }
        catch (final IOException exception) {
            _endPoint.shutdown();
        }
    }
}
