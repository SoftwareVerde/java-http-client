package com.softwareverde.http.websocket;

import com.amo.websocket.FrameType;
import com.amo.websocket.server.BasicFrame;
import com.amo.websocket.server.BasicFrameReader;
import com.amo.websocket.server.BasicFrameWriter;
import com.amo.websocket.server.PongFrame;
import com.softwareverde.util.ByteUtil;
import com.softwareverde.util.Util;
import org.eclipse.jetty.websocket.WebSocketBuffers;
import org.eclipse.jetty.websocket.WebSocketConnectionRFC6455;

import java.io.IOException;
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

    protected final BasicFrameReader _webSocketReader;
    protected final BasicFrameWriter _webSocketWriter;

    protected final Runnable _pingRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("running");
            final Thread thread = Thread.currentThread();
            try {
                while (true) {
                    final Long pingInterval = _pingInterval;
                    if (pingInterval == null) { break; }

                    Thread.sleep(pingInterval);

                    final long pingNonce = (int) (Math.random() * Integer.MAX_VALUE);
                    final byte[] pingNonceBytes = ByteUtil.longToBytes(pingNonce);

                    synchronized (_webSocketWriter) {
                        _webSocketWriter.write(new BasicFrame(false, false, false, false, false, FrameType.PING_FRAME, (byte) pingNonceBytes.length, null, pingNonceBytes));
                        System.out.println("websocketwriter wrote " + pingNonce);
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
    protected Long _pingInterval = 15000L;

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
            System.out.println("started ping thread");
        }
    }

    protected void _runOnSeparateThread(final Runnable runnable) {
        (new Thread(runnable)).start();
    }

    protected void _close(final int code, final String message) {
        try {
            final InputStream inputStream = _connectionLayer.getInputStream();
            System.out.println("closing " + inputStream);
            inputStream.close();
        }
        catch (final Exception exception) { }

        try {
            final OutputStream outputStream = _connectionLayer.getOutputStream();
            System.out.println("closing " + outputStream);
            outputStream.close();
        }
        catch (final Exception exception) { }

        try {
            final Socket socket = _connectionLayer.getSocket();
            System.out.println("closing " + socket);
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
            System.out.println("got websocket");
        }
        catch (final Exception exception) { }

        final WebSocketBuffers webSocketBuffers = new WebSocketBuffers(_maxPacketByteCount);

        final SocketStreams socketStreams = new SocketStreams(_connectionLayer);
        // try { socketStreams.setMaxIdleTime(0); } catch (final Exception exception) { }

        _webSocketReader = new BasicFrameReader(socketStreams.getInputStream(), webSocketBuffers.getBufferSize()) {};
        _webSocketWriter = new BasicFrameWriter(socketStreams.getOutputStream());
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

    public void startListening() throws IOException {
        _webSocketReader.read();
    }

    public void sendMessage(final String message) {
        synchronized (_webSocketWriter) {
            try {
                _webSocketWriter.write(new BasicFrame(false, false, false, false, false, FrameType.TEXT_FRAME, (byte) message.length(), null, null));
            }
            catch (final Exception exception) {
                _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
            }
        }
    }

    public void sendMessage(final byte[] bytes) {
        synchronized (_webSocketWriter) {
            try {
                _webSocketWriter.write(new BasicFrame(false, false, false, false, false, FrameType.TEXT_FRAME, (byte) bytes.length, null, bytes));
            }
            catch (final Exception exception) {
                _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
            }
        }
    }

    public void sendPing(final byte[] pingNonce) {
        try {
            synchronized (_webSocketWriter) {
                _webSocketWriter.write(new BasicFrame(false, false, false, false, false, FrameType.PING_FRAME, (byte) pingNonce.length, null, pingNonce));
            }
        }
        catch (final Exception exception) {
            _close(WebSocketConnectionRFC6455.CLOSE_NO_CODE, "");
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