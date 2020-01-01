package com.magabe.net;


abstract class MTcpInputStreamHandler implements Runnable {

    private MTcpSocket socket;

    public MTcpInputStreamHandler(MTcpSocket socket) throws NullPointerException {
        if (socket == null) {
            throw new NullPointerException("param socket is null");
        }
        this.socket = socket;
    }

    public void run() {
        while (!socket.isClosed()) {
            handleInput(socket);
        }
    }

    public MTcpSocket getSocket() {
        return socket;
    }

    public abstract void handleInput(MTcpSocket socket);
}
