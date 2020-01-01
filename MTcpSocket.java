package com.magabe.net;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

public class MTcpSocket extends MTcpSocketHelper {
    private static final String CONNECTION_RESET = "java.net.SocketException: Connection reset";//TODO IF DISCONNECTED DOESNT WORK,CHANGES TO THIS MAY BE THE COURSE
    private Socket mSocket;
    private InputStream mTcpSocketInputStream;
    private OutputStream mTcpSocketOutputStream;
    private DisconnectedCallback mDisconnectedCallback;

    public MTcpSocket(Socket socket) throws NullPointerException {
        if (socket == null) {
            throw new NullPointerException("param socket is null");
        }

        this.mSocket = socket;

        mTcpSocketInputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                try {
                    int i = mSocket.getInputStream().read();
                    if (i == -1) disconnected();
                    return i;
                } catch (IOException e) {
                    if (mSocket.isConnected() && (e.toString().equals(CONNECTION_RESET) || (e instanceof SocketException))) {
                        disconnected();
                    }
                    throw new IOException(e);
                }
            }

            @Override
            public int read(byte b[], int off, int len) throws IOException {
                try {
                    int i = mSocket.getInputStream().read(b, off, len);
                    if (i == -1) disconnected();
                    return i;
                } catch (IOException e) {
                    if (mSocket.isConnected() && (e.toString().equals(CONNECTION_RESET) || (e instanceof SocketException))) {
                        disconnected();
                    }
                    throw new IOException(e);
                }
            }

            @Override
            public long skip(long n) throws IOException {
                return mSocket.getInputStream().skip(n);
            }

            @Override
            public int available() throws IOException {
                return mSocket.getInputStream().available();
            }

            @Override
            public void close() throws IOException {
                mSocket.getInputStream().close();
            }

            @Override
            public void mark(int readlimit) {
                try {
                    mSocket.getInputStream().mark(readlimit);
                } catch (IOException e) {
                }
            }

            @Override
            public void reset() throws IOException {
                mSocket.getInputStream().reset();
            }

            @Override
            public boolean markSupported() {
                try {
                    return mSocket.getInputStream().markSupported();
                } catch (IOException e) {
                    return false;
                }
            }
        };

    }//constructor

    private void disconnected() {
        if (mDisconnectedCallback != null) {
            forceCloseSocket(this);
            mDisconnectedCallback.onDisconnected(getInetAddress(), getPort());
        }
    }

    public MTcpSocket setDisconnectedCallback(DisconnectedCallback disconnectedCallback) {
        this.mDisconnectedCallback = disconnectedCallback;
        return this;
    }

    public void connect(SocketAddress endpoint) throws IOException {
        mSocket.connect(endpoint);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        mSocket.connect(endpoint, timeout);
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        mSocket.bind(bindpoint);
    }

    public InetAddress getInetAddress() {
        return mSocket.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return mSocket.getLocalAddress();
    }

    public int getPort() {
        return mSocket.getPort();
    }


    public int getLocalPort() {
        return mSocket.getLocalPort();
    }


    public SocketAddress getRemoteSocketAddress() {
        return mSocket.getRemoteSocketAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return mSocket.getLocalSocketAddress();
    }

    public SocketChannel getChannel() {
        return mSocket.getChannel();
    }


    public InputStream getInputStream() {
        return mTcpSocketInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        if (mSocket.getOutputStream() == null) {
            throw new IOException("Corrupted socket OutputStream");
        } else {
            return mSocket.getOutputStream();
        }
    }

    public boolean getTcpNoDelay() throws SocketException {
        return mSocket.getTcpNoDelay();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        mSocket.setTcpNoDelay(on);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        mSocket.setSoLinger(on, linger);
    }

    public int getSoLinger() throws SocketException {
        return mSocket.getSoLinger();
    }


    public void sendUrgentData(int data) throws IOException {
        mSocket.sendUrgentData(data);
    }

    public boolean getOOBInline() throws SocketException {
        return mSocket.getOOBInline();
    }

    public void setOOBInline(boolean on) throws SocketException {
        mSocket.setOOBInline(on);
    }

    public int getSoTimeout() throws SocketException {
        return mSocket.getSoTimeout();
    }

    public void setSoTimeout(int timeout) throws SocketException {
        mSocket.setSoTimeout(timeout);
    }

    public int getSendBufferSize() throws SocketException {
        return mSocket.getSendBufferSize();
    }

    public void setSendBufferSize(int size)
            throws SocketException {
        mSocket.setSendBufferSize(size);
    }

    public int getReceiveBufferSize()
            throws SocketException {
        return mSocket.getReceiveBufferSize();
    }

    public void setReceiveBufferSize(int size)
            throws SocketException {
        mSocket.setReceiveBufferSize(size);
    }

    public boolean getKeepAlive() throws SocketException {
        return mSocket.getKeepAlive();
    }

    public boolean getReuseAddress() throws SocketException {
        return mSocket.getReuseAddress();
    }

    public void setReuseAddress(boolean on) throws SocketException {
        mSocket.setReuseAddress(on);
    }

    public void close() throws IOException {
        mSocket.close();
    }


    public void shutdownInput() throws IOException {
        mSocket.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        mSocket.shutdownOutput();
    }

    public String toString() {
        return "MTcpSocket :: " + mSocket.toString();
    }


    public boolean isConnected() {
        return mSocket.isConnected();
    }

    public boolean isBound() {
        return mSocket.isBound();
    }


    public boolean isClosed() {
        return mSocket.isClosed();
    }


    public boolean isInputShutdown() {
        return mSocket.isInputShutdown();
    }


    public boolean isOutputShutdown() {
        return mSocket.isOutputShutdown();
    }

    /**
     * interface DisconnectedCallback
     */
    public interface DisconnectedCallback {
        void onDisconnected(InetAddress address, int port);
    }
}
