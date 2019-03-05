package com.softwareverde.http.websocket;

import com.softwareverde.util.ByteBuffer;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.EndPoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

class SocketStreams implements EndPoint {
    protected final AtomicBoolean _isShutdown = new AtomicBoolean(false);
    protected final ByteBuffer _packetBuffer = new ByteBuffer();

    protected final Socket _socket;
    protected final InputStream _inputStream;
    protected final OutputStream _outputStream;

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

    // @Override
    // public boolean isOutputShutdown() {
    //     return _isShutdown.get();
    // }

    // @Override
    // public void shutdownInput() throws IOException {
    //     _shutdown();
    // }

    @Override
    public boolean isInputShutdown() {
        return _isShutdown.get();
    }

    // @Override
    // public void close() throws IOException {
    //     _shutdown();
    // }

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
        try {
            final byte[] bytes = buffer.asArray();
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
    }

    // @Override
    // public int flush(final Buffer header, final Buffer buffer, final Buffer trailer) throws IOException {
    //     return (this.flush(header) + this.flush(buffer) + this.flush(trailer));
    // }

    // @Override
    // public String getLocalAddr() {
    //     return _socket.getLocalAddress().getHostAddress();
    // }

    // @Override
    // public String getLocalHost() {
    //     return _socket.getLocalAddress().getHostName();
    // }

    // @Override
    // public int getLocalPort() {
    //     return _socket.getLocalPort();
    // }

    // @Override
    // public String getRemoteAddr() {
    //     return _socket.getInetAddress().getHostAddress();
    // }

    // @Override
    // public String getRemoteHost() {
    //     return _socket.getInetAddress().getHostName();
    // }

    // @Override
    // public int getRemotePort() {
    //     return _socket.getPort();
    // }

    @Override
    public boolean isBlocking() {
        return true;
    }

    // @Override
    // public boolean blockReadable(final long millisecs) {
    //     return true;
    // }

    @Override
    public boolean blockWritable(final long millisecs) {
        return true;
    }

    @Override
    public boolean isOpen() {
        return (! _isShutdown.get());
    }

    // @Override
    // public Object getTransport() {
    //     return null;
    // }

    // @Override
    // public void flush() throws IOException {
    //     _outputStream.flush();
    // }

    @Override
    public int getMaxIdleTime() {
        return Integer.MAX_VALUE;
    }

    // @Override
    // public void setMaxIdleTime(final int timeMs) { }
}
