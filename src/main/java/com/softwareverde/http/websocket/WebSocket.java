package com.softwareverde.http.websocket;

import com.softwareverde.util.Util;
import org.eclipse.jetty.websocket.WebSocketBuffers;
import org.eclipse.jetty.websocket.WebSocketConnectionRFC6455;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

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

    protected static final Integer IGNORE_READS_SO_TIMEOUT = 100;

    protected final Long _webSocketId;
    protected final Mode _mode;
    protected final Integer _maxPacketByteCount;
    protected final ConnectionLayer _connectionLayer;

    protected final WebSocketReader _webSocketReader;
    protected final WebSocketWriter _webSocketWriter;

    protected MessageReceivedCallback _messageReceivedCallback;
    protected BinaryMessageReceivedCallback _binaryMessageReceivedCallback;
    protected ConnectionClosedCallback _connectionClosedCallback;

    protected final AtomicBoolean _closedCallbackInvoked = new AtomicBoolean(false);

    protected void _runOnSeparateThread(final Runnable runnable) {
        (new Thread(runnable)).start();
    }

    protected void _close(final int code, final String message) {
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

        if (! _closedCallbackInvoked.getAndSet(true)) {
            final ConnectionClosedCallback connectionClosedCallback = _connectionClosedCallback;
            if (connectionClosedCallback != null) {
                _runOnSeparateThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionClosedCallback.onClose(code, message);
                    }
                });
            }
        }
    }

    public WebSocket(final Long webSocketId, final Mode mode, final ConnectionLayer connectionLayer, final Integer maxPacketByteCount) {
        _webSocketId = webSocketId;
        _mode = mode;
        _maxPacketByteCount = maxPacketByteCount;
        _connectionLayer = connectionLayer;

        try {
            // Setting the Socket Timeout prevents writes from being blocked by unprocessed reads.
            final Socket socket = connectionLayer.getSocket();
            socket.setSoTimeout(IGNORE_READS_SO_TIMEOUT);
        }
        catch (final Exception exception) { }

        final WebSocketBuffers webSocketBuffers = new WebSocketBuffers(_maxPacketByteCount);

        final SocketStreams socketStreams = new SocketStreams(_connectionLayer);
        // try { socketStreams.setMaxIdleTime(0); } catch (final Exception exception) { }

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
                synchronized (_webSocketWriter) {
                    try {
                        _webSocketWriter.writePong(message);
                    }
                    catch (final Exception exception) {
                        _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
                    }
                }
            }

            @Override
            public void onPong(final byte[] message) { }

            @Override
            public void onClose(final int code, final String message) {
                _close(code, message);
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

    public void setSocketTimeout(final Integer socketTimeoutMs) {
        final Socket socket = _connectionLayer.getSocket();
        try {
            socket.setSoTimeout(socketTimeoutMs);
        }
        catch (final Exception exception) {
            // Nothing.
        }
    }

    public void startListening() {
        try { // Disable SO_TIMEOUT to remove read-lag since reads are not being ignored.
            final Socket socket = _connectionLayer.getSocket();

            final int soTimeout = socket.getSoTimeout();
            final boolean soTimeoutWasChanged = (! Util.areEqual(IGNORE_READS_SO_TIMEOUT, soTimeout));
            if (! soTimeoutWasChanged) {
                socket.setSoTimeout(0);
            }
        }
        catch (final Exception exception) {
            // Nothing.
        }

        _webSocketReader.start();
    }

    public void sendMessage(final String message) {
        synchronized (_webSocketWriter) {
            try {
                _webSocketWriter.writeMessage(message);
            }
            catch (final Exception exception) {
                _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
            }
        }
    }

    public void sendMessage(final byte[] bytes) {
        synchronized (_webSocketWriter) {
            try {
                _webSocketWriter.writeMessage(bytes);
            }
            catch (final Exception exception) {
                _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
            }
        }
    }

    public Integer getMaxPacketByteCount() {
        return _maxPacketByteCount;
    }

    public ConnectionLayer getConnectionLayer() {
        return _connectionLayer;
    }

    @Override
    public void close() {
        _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
    }
}
