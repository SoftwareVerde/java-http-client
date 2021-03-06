package com.softwareverde.http.websocket;

import com.softwareverde.util.ByteBuffer;
import org.eclipse.jetty.websocket.WSFrameHandler;
import org.eclipse.jetty.websocket.WebSocketBuffers;
import org.eclipse.jetty.websocket.WebSocketParserRFC6455;

import java.io.InputStream;
import java.net.SocketTimeoutException;

class WebSocketReader {
    public interface MessageReceivedCallback {
        void onTextMessage(String message);
        void onBinaryMessage(byte[] message);
        void onPing(byte[] message);
        void onPong(byte[] message);
        void onClose(int code, String message);
    }

    protected final WebSocketParserRFC6455 _webSocketParser;
    protected final MessageReceivedCallback _messageReceivedCallback;

    final ByteBuffer _packetBuffer;

    protected final Thread _readThread;

    public WebSocketReader(final WebSocket.Mode mode, final SocketStreams endPoint, final WebSocketBuffers webSocketBuffers, final MessageReceivedCallback messageReceivedCallback) {
        _messageReceivedCallback = messageReceivedCallback;

        final InputStream inputStream = endPoint.getInputStream();
        _packetBuffer = endPoint.getPacketBuffer();

        final WSFrameHandler frameHandler = new WSFrameHandler(webSocketBuffers.getBufferSize(), new WSFrameHandler.CloseSocketHandler() {
            @Override
            public void close(final int code, final String message) {
                _readThread.interrupt();
                try { inputStream.close(); } catch (final Exception exception) { }
                _messageReceivedCallback.onClose(code, message);
            }
        });

        frameHandler.setTextMessageCallback(new WSFrameHandler.Callback<String>() {
            @Override
            public void onMessage(final String message) {
                _messageReceivedCallback.onTextMessage(message);
            }
        });

        frameHandler.setBinaryMessageCallback(new WSFrameHandler.Callback<byte[]>() {
            @Override
            public void onMessage(final byte[] message) {
                _messageReceivedCallback.onBinaryMessage(message);
            }
        });

        frameHandler.setPingMessageCallback(new WSFrameHandler.Callback<byte[]>() {
            @Override
            public void onMessage(final byte[] message) {
                _messageReceivedCallback.onPing(message);
            }
        });

        frameHandler.setPongMessageCallback(new WSFrameHandler.Callback<byte[]>() {
            @Override
            public void onMessage(final byte[] message) {
                _messageReceivedCallback.onPong(message);
            }
        });

        _webSocketParser = new WebSocketParserRFC6455(webSocketBuffers, endPoint, frameHandler, (mode == WebSocket.Mode.SERVER));
        _webSocketParser.setFakeFragments(true);

        _readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer;
                    synchronized (_packetBuffer) {
                        buffer = _packetBuffer.getRecycledBuffer();
                    }

                    while (true) {
                        do {
                            Thread.sleep(100); // Prioritize writes since reading will block writes...
                        } while ( (endPoint.getQueuedWriteByteCount() > 0) && (inputStream.available() < 1) );

                        final int readByteCount;
                        if (endPoint.isInputShutdown()) { break; }
                        try {
                            readByteCount = inputStream.read(buffer);
                        }
                        catch (final SocketTimeoutException socketTimeoutException) { continue; }
                        if (_readThread.isInterrupted()) { break; }

                        if (readByteCount > 0) {
                            synchronized (_packetBuffer) {
                                _packetBuffer.appendBytes(buffer, readByteCount);
                                _webSocketParser.parseNext();
                                buffer = _packetBuffer.getRecycledBuffer();
                            }
                        }
                    }
                }
                catch (final Exception exception) { }

                endPoint.shutdown();
            }
        });
    }

    public void start() {
        if (! _readThread.isAlive()) {
            _readThread.start();
        }
    }
}
