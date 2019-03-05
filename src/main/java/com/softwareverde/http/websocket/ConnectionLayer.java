package com.softwareverde.http.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionLayer {
    public static ConnectionLayer newConnectionLayer(final Socket socket) {
        try {
            return new ConnectionLayer(false, socket, socket.getInputStream(), socket.getOutputStream());
        }
        catch (final IOException exception) {
            return null;
        }
    }

    public static ConnectionLayer newSecureConnectionLayer(final Socket socket, final InputStream sslInputStream, final OutputStream sslOutputStream) {
        return new ConnectionLayer(true, socket, sslInputStream, sslOutputStream);
    }

    protected final Boolean _isSecure;
    protected final Socket _socket;
    protected final InputStream _inputStream;
    protected final OutputStream _outputStream;

    protected ConnectionLayer(final Boolean isSecure, final Socket socket, final InputStream inputStream, final OutputStream outputStream) {
        _isSecure = isSecure;
        _socket = socket;
        _inputStream = inputStream;
        _outputStream = outputStream;
    }

    public Boolean isSecure() {
        return _isSecure;
    }

    public Socket getSocket() {
        return _socket;
    }

    public InputStream getInputStream() {
        return _inputStream;
    }

    public OutputStream getOutputStream() {
        return _outputStream;
    }
}