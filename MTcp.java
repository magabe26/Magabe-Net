package com.magabe.net;

import java.net.InetAddress;

public abstract class MTcp  extends MTcpSocketHelper {
    protected static final int FAILED_TO_START_ERROR = 0xcb;//200
    protected static final int CONNECTION_ERROR = 0xc9;//201
    protected static final int FAILED_TO_RECONNECT_ERROR = 0xca;//202
    private int mPort = 0;
    private String mError = "";
    private InetAddress mServerHostAddress = null;
    private MTcpThreadPool mTcpOutputStreamHandlerThreadPool;
    private MTcpThreadPool mTcpInputStreamHandlerThreadPool;
    private volatile boolean mAllThreadPoolThreadsTerminated = false;

    protected synchronized boolean areAllThreadPoolsThreadsTerminated() {
        /**Works
         * logic : if invoked twice that means all threads from both
         * TcpOutputStreamHandlerThreadPool and TcpOutputStreamHandlerThreadPool are terminated [ or not]
         **/
        boolean tmp = mAllThreadPoolThreadsTerminated;
        mAllThreadPoolThreadsTerminated = !mAllThreadPoolThreadsTerminated;
        return tmp;
    }

    protected MTcpThreadPool getTcpInputStreamHandlerThreadPool() {
        return mTcpInputStreamHandlerThreadPool;
    }

    protected MTcpThreadPool getTcpOutputStreamHandlerThreadPool() {
        return mTcpOutputStreamHandlerThreadPool;
    }

    protected void setTcpOutputStreamHandlerThreadPool(MTcpThreadPool tcpOutputStreamHandlerThreadPool) {
        this.mTcpOutputStreamHandlerThreadPool = tcpOutputStreamHandlerThreadPool;
        this.mTcpOutputStreamHandlerThreadPool.setThreadPoolName("TcpOutputStreamHandlerThreadPool");
    }

    protected void setTcpInputStreamHandlerThreadPool(MTcpThreadPool tcpInputStreamHandlerThreadPool) {
        this.mTcpInputStreamHandlerThreadPool = tcpInputStreamHandlerThreadPool;
        this.mTcpInputStreamHandlerThreadPool.setThreadPoolName("TcpInputStreamHandlerThreadPool");
    }

    public void postOutputStreamTask(Runnable outputTask) {
        if (mTcpOutputStreamHandlerThreadPool != null)
            mTcpOutputStreamHandlerThreadPool.post(outputTask);
    }


    protected void stopThreadPools() {
        mTcpInputStreamHandlerThreadPool.stopThreads();
        mTcpOutputStreamHandlerThreadPool.stopThreads();
    }

    public synchronized boolean hasActiveThreads() {
        return (mTcpInputStreamHandlerThreadPool.hasActiveThreads() ||  mTcpOutputStreamHandlerThreadPool.hasActiveThreads());
    }

    protected void startThreadPools() {
        mTcpInputStreamHandlerThreadPool.startThreads();
        mTcpOutputStreamHandlerThreadPool.startThreads();
    }

    public String getError() {
        return mError;
    }

    protected void setError(String error) {
        mError = error;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int mPort) throws IllegalArgumentException {
        if (mPort > 0 && mPort <= 65535) {
            this.mPort = mPort;
        } else {
            throw new IllegalArgumentException("Illegal mPort, Port must be between 0 and 65536 exclusive,use port > 1024  for non standard service");
        }
    }


    public InetAddress getServerHostAddress() {
        return mServerHostAddress;
    }

    public void setServerHostAddress(InetAddress serverHostAddress) {
        this.mServerHostAddress = serverHostAddress;
    }


    /**
     * you must implement, and use input stream readers like BufferedReader that block
     * do not use loops inside its implementation
     *
     * @param socket
     */
    protected abstract void onHandleInput(MTcpSocket socket);

    protected abstract void onError(int errorCode, String error);

}
