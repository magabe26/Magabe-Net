package com.magabe.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * NOTE user or subclass of this class,should avoid sharing OutputStream or InputStream
 * created from socket().getOutputStream() or socket().getInputStream() ,
 * to avoid race conditions ,each input or output operation should use its own copy
 * see test examples
 */
public abstract class MTcpServer extends MTcp {

    private int mMaxNoOfClients = 50;
    private IncomingConnectionManager incomingConnectionManager;
    private DisconnectionHandler disconnectionHandler = new DisconnectionHandler();

    public MTcpServer(int port) throws IllegalArgumentException {
        this(port, 50);
    }

    public MTcpServer(int port, int maxNoOfClients) throws IllegalArgumentException {
        this(null, port, maxNoOfClients);
    }

    public MTcpServer(InetAddress hostAddress, int port, int maxNoOfClients) throws IllegalArgumentException {
        setPort(port);
        setMaxNoOfClients(maxNoOfClients);
        setServerHostAddress(hostAddress);
        setTcpInputStreamHandlerThreadPool(new MTcpThreadPool(maxNoOfClients));
        setTcpOutputStreamHandlerThreadPool(new MTcpThreadPool(maxNoOfClients));

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
                    finalizeStopOperation();
                }
            }
        };
        getTcpInputStreamHandlerThreadPool().setThreadsStatusCallbacks(threadsStatusCallbacks);
        getTcpOutputStreamHandlerThreadPool().setThreadsStatusCallbacks(threadsStatusCallbacks);
    }

    private void finalizeStopOperation() {
        incomingConnectionManager = null;
        setError("");
        onStopped();
    }

    /**
     * Get the max number of client, the server can handle
     *
     * @return
     */
    public int getMaxNoOfClients() {
        return mMaxNoOfClients;
    }

    /**
     * Set the max number of client, the server can handle
     *
     * @param maxNoOfClients
     * @throws IllegalArgumentException
     */
    public void setMaxNoOfClients(int maxNoOfClients) throws IllegalArgumentException {
        if (maxNoOfClients < 1) {
            throw new IllegalArgumentException("maxNoOfClients must be >= 1");
        }
        mMaxNoOfClients = maxNoOfClients;
    }


    public synchronized boolean isStarted() {
        return (incomingConnectionManager != null);
    }

    public void resumeAccepting() {
        if (isStarted()) {
            incomingConnectionManager.resumeAccepting();
        }
    }

    public boolean isAccepting() {
        if (isStarted()) {
            return incomingConnectionManager.isAccepting();
        } else
            return false;
    }

    public void pauseAccepting() {
        if (isStarted()) {
            incomingConnectionManager.pauseAccepting();
        }
    }

    /**
     * stop the server
     */
    public synchronized void stop() {
        incomingConnectionManager.stopManager();
    }

    /**
     * start the server
     */
    public synchronized void start() {
        if (isStarted()) {
            return;
        }

        //avoid network on ui thread exception in android
        incomingConnectionManager = new IncomingConnectionManager();
        incomingConnectionManager.startManager();
    }

    /**
     * called when server started
     */
    protected void onStarted() {
    }

    /**
     * called when server stopped
     */
    protected void onStopped() {
    }

    /**
     * called when a new client is connected
     *
     * @param socket
     */
    protected abstract void onClientConnected(MTcpSocket socket);

    /**
     * Only work when a reading thread is reading from INPUT STREAM in a while loop
     *
     * @param address
     * @param port
     */
    protected abstract void onClientDisconnected(InetAddress address, int port);

    /**
     * You must implement, and use input stream readers like BufferedReader that block
     * do not use loops inside its implementation
     */
    protected abstract void onHandleInput(MTcpSocket socket);

    protected abstract void onError(int errorCode, String error);

    /**
     * Class DisconnectionHandler
     */
    private class DisconnectionHandler implements MTcpSocket.DisconnectedCallback {

        @Override
        public void onDisconnected(InetAddress address, int port) {
            MTcpServer.this.onClientDisconnected(address, port);
        }
    }

    /**
     * Class IncomingConnectionManager
     */
    private class IncomingConnectionManager extends Thread {
        private ServerSocket mServerSocket;
        private boolean mAccepting = true;
        private boolean running = false;
        private volatile boolean mThreadsStarted = false;

        private void startManager() {
            running = true;
            start();
        }

        void stopManager() {
            running = false;
            mAccepting = false;
            closeServerSocket();
            interrupt();
        }

        synchronized void resumeAccepting() {
            mAccepting = true;
            notify();
        }

        public boolean isAccepting() {
            return mAccepting;
        }

        public void pauseAccepting() {
            mAccepting = false;
        }

        private void closeServerSocket() {
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mServerSocket = null;
                }
            }
        }

        @Override
        public void run() {

            mainLoop:
            while (running) {

                try {

                    if (getServerHostAddress() != null) {
                        mServerSocket = new ServerSocket(getPort(), mMaxNoOfClients, getServerHostAddress());
                    } else {
                        mServerSocket = new ServerSocket(getPort(), mMaxNoOfClients);
                        setServerHostAddress(mServerSocket.getInetAddress());
                    }

                } catch (IOException e) {
                    setError(e.getMessage());
                    onError(FAILED_TO_START_ERROR, getError());
                    break;
                }

                if (!mServerSocket.isBound()) {
                    setError("Failed to bind socket on Port " + getPort());
                    onError(FAILED_TO_START_ERROR, getError());
                    break;
                } else {

                    if (!mThreadsStarted) {
                        mThreadsStarted = true;
                        startThreadPools();
                        onStarted();
                    }

                    while (mServerSocket != null) {

                        if (!running) {
                            closeServerSocket();
                            break mainLoop;
                        } else if (!mAccepting) {
                            closeServerSocket();
                            synchronized (this) {
                                try {
                                    wait();
                                } catch (InterruptedException e) {
                                    //e.printStackTrace();
                                }
                            }
                            break;
                        }

                        try {
                            Socket socket = mServerSocket.accept();

                            if (socket != null) {
                                MTcpSocket sock = new MTcpSocket(socket).setDisconnectedCallback(disconnectionHandler);
                                getTcpInputStreamHandlerThreadPool().post(new MTcpInputStreamHandler(sock) {
                                    @Override
                                    public void handleInput(MTcpSocket socket) {
                                        onHandleInput(socket);
                                    }
                                });
                                onClientConnected(sock);
                            }

                        } catch (IOException e) {
                        }

                    }

                }

            }

            if (mThreadsStarted) {
                if (hasActiveThreads()) {
                    stopThreadPools();
                } else {
                    finalizeStopOperation();
                }
            }
        }


    }

}
