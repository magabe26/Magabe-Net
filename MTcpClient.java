package com.magabe.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * NOTE user or subclass of this class,should avoid sharing OutputStream or InputStream
 * created from socket().getOutputStream() or socket().getInputStream() ,
 * to avoid race conditions ,each input or output operation should use its own copy
 * see test examples
 */
public abstract class MTcpClient extends MTcp {

    private MTcpSocket mSocket = null;
    private DisconnectionHandler disconnectionHandler = new DisconnectionHandler();
    private volatile boolean mIsConnecting = false;
    private volatile boolean mIsReconnectingScheduled = false;
    private volatile boolean mReConnectInvokedByConnect = false;

    private int mDisconnectionCount = 0;
    private volatile boolean mIDisconnect = false;

    public MTcpClient() {
        setTcpInputStreamHandlerThreadPool(new MTcpThreadPool());
        setTcpOutputStreamHandlerThreadPool(new MTcpThreadPool());

        MTcpThreadPool.ThreadsStatusCallbacks threadsStatusCallbacks = new MTcpThreadPool.ThreadsStatusCallbacks() {
            @Override
            public void onThreadStarted(int threadId) {
                //not used
            }

            @Override
            public void onThreadStopped(int threadId) {
                //not used
            }

            @Override
            public void onAllThreadsStopped() {
                if (areAllThreadPoolsThreadsTerminated()) {
                    forceCloseSocket(mSocket);
                    mSocket = null;
                    if (!mIDisconnect) {
                        ++mDisconnectionCount;//means the server keeps disconnecting me, when i reconnect
                    } else {
                        mDisconnectionCount = 0;
                        mIDisconnect = false;
                    }
                    if (mDisconnectionCount == 6) { //raise error , after 5 attempts
                        mDisconnectionCount = 0;
                        String error = "Unknown connection error";
                        setError(error);
                        onError(CONNECTION_ERROR, error);
                    } else {
                        MTcpClient.this.onDisconnected();
                    }
                    mIsConnecting = false;
                    if (mIsReconnectingScheduled) {
                        _reConnect();
                        mIsReconnectingScheduled = false; //reset var
                    }
                }
            }
        };

        getTcpInputStreamHandlerThreadPool().setThreadsStatusCallbacks(threadsStatusCallbacks);
        getTcpOutputStreamHandlerThreadPool().setThreadsStatusCallbacks(threadsStatusCallbacks);

    }

    public void connect(String address, int port) {
        try {
            connect(InetAddress.getByName(address), port);
        } catch (UnknownHostException e) {
            String error = e.getMessage();
            setError(error);
            onError(CONNECTION_ERROR, error);
            mIsConnecting = false;
        }
    }

    private void connect(InetAddress address, int port) {
        if (isConnected()) return;
        setPort(port);
        setServerHostAddress(address);
        mReConnectInvokedByConnect = true;
        _reConnect();
    }

    /**
     * reconnet after 2 sec
     */
    public void reConnect() {//2000mills = 2sec
        reConnect(2000);
    }

    public void reConnect(int afterNMillis) {//note:1000mills = 1sec
        Timer timer = new Timer("timer", false);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                _reConnect();
            }
        }, afterNMillis);
    }

    private synchronized void _reConnect() {
        if (isConnected()) return;

        if (mIsConnecting) {
            if (!mReConnectInvokedByConnect) {
                mIsReconnectingScheduled = true;
            }
            return;
        } else {
            mIsConnecting = true;
        }

        mReConnectInvokedByConnect = false;//reset var

        if (mSocket != null) { //just in case,help to prevent multiple connection from this same object
            forceCloseSocket(mSocket);
        }

        if (getServerHostAddress() != null && getPort() != 0) {
            //avoid network on ui thread exception in android
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        mSocket = new MTcpSocket(new Socket(getServerHostAddress(), getPort()))
                                .setDisconnectedCallback(disconnectionHandler);
                        startThreadPools();
                        getTcpInputStreamHandlerThreadPool().post(new MTcpInputStreamHandler(mSocket) {
                            @Override
                            public void handleInput(MTcpSocket socket) {
                                onHandleInput(socket);
                            }
                        });
                        onConnected();
                    } catch (IOException e) {
                        String error = e.getMessage();
                        setError(error);
                        onError(CONNECTION_ERROR, error);
                        if (hasActiveThreads()) {
                            stopThreadPools();
                        } else {
                            mIsConnecting = false;
                        }
                    }
                }
            }.start();

        } else {
            String error = "Failed to reConnect";
            setError(error);
            onError(FAILED_TO_RECONNECT_ERROR, error);
            mIsConnecting = false;
        }
    }

    public InputStream getInputStream() {
        return mSocket.getInputStream();
    }

    /**
     * it may return null,check it first before using the steam
     *
     * @return
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException {
        return mSocket.getOutputStream();
    }

    protected abstract void onConnected();

    public void disconnect() {
        if (!isConnected()) return;
        mIDisconnect = true;
        stopThreadPools();
    }

    public synchronized boolean isConnected() {//do not change this method
        if (mSocket == null) {
            return false;
        } else {
            try {
                mSocket.getOutputStream();//check any exception
                return true;
            } catch (IOException e) {
                return false;
            }
        }

    }


    protected abstract void onDisconnected();

    private class DisconnectionHandler implements MTcpSocket.DisconnectedCallback {

        @Override
        public void onDisconnected(InetAddress address, int port) {
            stopThreadPools();
        }
    }

}
