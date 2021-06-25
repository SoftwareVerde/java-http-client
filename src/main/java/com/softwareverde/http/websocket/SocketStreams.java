package com.softwareverde.http.websocket;

import com.softwareverde.util.ByteBuffer;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.EndPoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class SocketStreams implements EndPoint {
    protected final AtomicBoolean _isShutdown = new AtomicBoolean(false);
    protected final ByteBuffer _packetBuffer = new ByteBuffer();

    protected final Socket _socket;
    protected final InputStream _inputStream;
    protected final OutputStream _outputStream;

    protected final AtomicInteger _queuedWriteByteCount = new AtomicInteger(0);

    protected void _shutdown() {
        _isShutdown.set(true);

        try {
            _inputStream.close();
        }
        catch (final Exception exception) { }

        try {
            _outputStream.close();
        }
        catch (final Exception exception) { }

        try {
            _socket.close();
        }
        catch (final Exception exception) { }
    }

    public SocketStreams(final ConnectionLayer connectionLayer) {
        _socket = connectionLayer.getSocket();
        _inputStream = connectionLayer.getInputStream();
        _outputStream = connectionLayer.getOutputStream();
    }

    public void shutdown() {
        _shutdown();
    }

    public ByteBuffer getPacketBuffer() {
        return _packetBuffer;
    }

    public InputStream getInputStream() {
        return _inputStream;
    }

    public OutputStream getOutputStream() {
        return _outputStream;
    }

    @Override
    public void shutdownOutput() {
        _shutdown();
    }

    @Override
    public boolean isInputShutdown() {
        return _isShutdown.get();
    }

    @Override
    public int fill(final Buffer buffer) {

        final byte[] bytes;
        synchronized (_packetBuffer) {
            final int byteCount = _packetBuffer.getByteCount();
            bytes = _packetBuffer.readBytes(byteCount);
        }

        if (bytes.length > 0) {
            buffer.put(bytes);
        }
        return bytes.length;
    }

    @Override
    public int flush(final Buffer buffer) throws IOException {
        final byte[] bytes = buffer.asArray();
        _queuedWriteByteCount.addAndGet(bytes.length);

        try {
            if (bytes.length > 0) {
                _outputStream.write(bytes);
                buffer.clear();
            }
            return bytes.length;
        }
        catch (final IOException exception) {
            _shutdown();
            throw exception;
        }
        finally {
            _queuedWriteByteCount.addAndGet(-bytes.length);
        }
    }

    public Integer getQueuedWriteByteCount() {
        return _queuedWriteByteCount.get();
    }

    @Override
    public boolean isOpen() {
        return (! _isShutdown.get());
    }
}
