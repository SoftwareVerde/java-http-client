package com.softwareverde.http.websocket;

import org.eclipse.jetty.websocket.WebSocketBuffers;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class WebSocket implements AutoCloseable {
    public enum Mode {
        CLIENT, SERVER
    }

    public static final Integer DEFAULT_MAX_PACKET_BYTE_COUNT = 8192;

    public interface MessageReceivedCallback {
        void onMessage(String message);
    }

    public interface BinaryMessageReceivedCallback {
        void onMessage(byte[] bytes);
    }

    public interface ConnectionClosedCallback {
        void onClose(int code, String message);
    }

    protected final Long _webSocketId;
    protected final Mode _mode;
    protected final Integer _maxPacketByteCount;
    protected final ConnectionLayer _connectionLayer;

    protected final WebSocketReader _webSocketReader;
    protected final WebSocketWriter _webSocketWriter;

    protected MessageReceivedCallback _messageReceivedCallback;
    protected BinaryMessageReceivedCallback _binaryMessageReceivedCallback;
    protected ConnectionClosedCallback _connectionClosedCallback;

    public WebSocket(final Long webSocketId, final Mode mode, final ConnectionLayer connectionLayer, final Integer maxPacketByteCount) {
        _webSocketId = webSocketId;
        _mode = mode;
        _maxPacketByteCount = maxPacketByteCount;
        _connectionLayer = connectionLayer;

        try {
            // Setting the Socket Timeout prevents writes from being blocked by unprocessed reads.
            final Socket socket = connectionLayer.getSocket();
            socket.setSoTimeout(100);
        }
        catch (final Exception exception) { }

        final WebSocketBuffers webSocketBuffers = new WebSocketBuffers(_maxPacketByteCount);

        final SocketStreams socketStreams = new SocketStreams(_connectionLayer);

        _webSocketReader = new WebSocketReader(_mode, socketStreams, webSocketBuffers, new WebSocketReader.MessageReceivedCallback() {
            @Override
            public void onTextMessage(final String message) {
                final MessageReceivedCallback messageReceivedCallback = _messageReceivedCallback;
                if (messageReceivedCallback != null) {
                    messageReceivedCallback.onMessage(message);
                }
            }

            @Override
            public void onBinaryMessage(final byte[] message) {
                final BinaryMessageReceivedCallback binaryMessageReceivedCallback = _binaryMessageReceivedCallback;
                if (binaryMessageReceivedCallback != null) {
                    binaryMessageReceivedCallback.onMessage(message);
                }
            }

            @Override
            public void onPing(final byte[] message) {
                _webSocketWriter.writePong(message);
            }

            @Override
            public void onClose(final int code, final String message) {
                final ConnectionClosedCallback connectionClosedCallback = _connectionClosedCallback;
                if (connectionClosedCallback != null) {
                    connectionClosedCallback.onClose(code, message);
                }
            }
        });

        _webSocketWriter = new WebSocketWriter(_mode, webSocketBuffers, socketStreams);
    }

    public void setMessageReceivedCallback(final MessageReceivedCallback messageReceivedCallback) {
        _messageReceivedCallback = messageReceivedCallback;
    }

    public void setBinaryMessageReceivedCallback(final BinaryMessageReceivedCallback binaryMessageReceivedCallback) {
        _binaryMessageReceivedCallback = binaryMessageReceivedCallback;
    }

    public void setConnectionClosedCallback(final ConnectionClosedCallback connectionClosedCallback) {
        _connectionClosedCallback = connectionClosedCallback;
    }

    public Long getId() {
        return _webSocketId;
    }

    public void startListening() {
        _webSocketReader.start();
    }

    public void sendMessage(final String message) {
        _webSocketWriter.writeMessage(message);
    }

    public void sendMessage(final byte[] bytes) {
        _webSocketWriter.writeMessage(bytes);
    }

    public Integer getMaxPacketByteCount() {
        return _maxPacketByteCount;
    }

    public ConnectionLayer getConnectionLayer() {
        return _connectionLayer;
    }

    @Override
    public void close() {
        try {
            final InputStream inputStream = _connectionLayer.getInputStream();
            inputStream.close();
        }
        catch (final Exception exception) { }

        try {
            final OutputStream outputStream = _connectionLayer.getOutputStream();
            outputStream.close();
        }
        catch (final Exception exception) { }

        try {
            final Socket socket = _connectionLayer.getSocket();
            socket.close();
        }
        catch (final Exception exception) { }
    }
}
