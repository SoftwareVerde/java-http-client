package com.softwareverde.http.websocket;

import com.softwareverde.util.ByteUtil;
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

    protected static final Integer DEFAULT_SO_TIMEOUT = 100;

    protected final Long _webSocketId;
    protected final Mode _mode;
    protected final Integer _maxPacketByteCount;
    protected final ConnectionLayer _connectionLayer;

    protected final WebSocketReader _webSocketReader;
    protected final WebSocketWriter _webSocketWriter;

    protected final Runnable _pingRunnable = new Runnable() {
        @Override
        public void run() {
            final Thread thread = Thread.currentThread();

            try {
                while (true) {
                    final Long pingInterval = _pingInterval;
                    if (pingInterval == null) { break; }

                    Thread.sleep(pingInterval);

                    final int pingNonce = (int) (Math.random() * Integer.MAX_VALUE);
                    final byte[] pingNonceBytes = ByteUtil.integerToBytes(pingNonce);

                    synchronized (_webSocketWriter) {
                        _webSocketWriter.writePing(pingNonceBytes);
                    }
                }
            }
            catch (final InterruptedException exception) { }
            catch (final Exception exception) {
                _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
            }
            finally {
                synchronized (_pingRunnable) {
                    if (_pingThread == thread) {
                        _pingThread = null;
                    }
                }
            }
        }
    };

    protected MessageReceivedCallback _messageReceivedCallback;
    protected BinaryMessageReceivedCallback _binaryMessageReceivedCallback;
    protected ConnectionClosedCallback _connectionClosedCallback;
    protected Thread _pingThread;
    protected Long _pingInterval = 60000L;

    protected final AtomicBoolean _closedCallbackInvoked = new AtomicBoolean(false);

    protected void _startPingThread() {
        synchronized (_pingRunnable) {
            if (_pingThread != null) {
                _pingThread.interrupt();
            }

            final Thread pingThread = new Thread(_pingRunnable);
            pingThread.setDaemon(true);
            pingThread.setName("WebSocket Ping Thread");
            pingThread.start();
            _pingThread = pingThread;
        }
    }

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

        synchronized (_pingRunnable) {
            final Thread pingThread = _pingThread;
            if (pingThread != null) {
                pingThread.interrupt();
            }
        }

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
            socket.setSoTimeout(DEFAULT_SO_TIMEOUT);
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
        _startPingThread();
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

    public void setPingInterval(final Long intervalMs) {
        final Long cleanedIntervalMs = (Util.coalesce(intervalMs) <= 0L ? null : intervalMs);
        _pingInterval = cleanedIntervalMs;

        if (cleanedIntervalMs != null) {
            _startPingThread();
        }
        else {
            final Thread pingThread = _pingThread;
            if (pingThread != null) {
                pingThread.interrupt();
            }
        }
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
