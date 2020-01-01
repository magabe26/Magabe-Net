package com.magabe.net.tests.zmgame;

import com.magabe.net.MTcpClient;
import com.magabe.net.MTcpSocket;

import java.io.*;

public class ZMGameClient2 extends MTcpClient {

    public ZMGameClient2() {
        connect("127.0.0.1", 2100);
    }

    @Override
    protected void onHandleInput(MTcpSocket socket) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            try {
                Object obj = objectInputStream.readObject();
                processObject(obj);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    @Override
    protected void onConnected() {
        System.out.print("connected  ");
        send(" HELLO SERVER");
    }

    @Override
    protected void onDisconnected() {
        System.out.print("disconnected ");
    }

    @Override
    protected void onError(int errorCode, String error) {
        if (errorCode == CONNECTION_ERROR)
            System.out.print("Connection error :- " + error);
    }

    private void processObject(Object obj) {

    }


    public void send(final Object obj) {
        postOutputStreamTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ObjectOutputStream out = new ObjectOutputStream(getOutputStream());
                    out.writeObject(obj);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
