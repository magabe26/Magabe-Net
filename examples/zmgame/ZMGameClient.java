package com.magabe.net.tests.zmgame;

import com.magabe.net.MTcpClient;
import com.magabe.net.MTcpSocket;

import java.io.*;

public class ZMGameClient {
    private static final String TAG = "ZMGameClient";
    private MTcpClient client;

    public ZMGameClient() {
        client = new MTcpClient() {
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
                System.out.println("Connected !");
            }

            @Override
            protected void onDisconnected() {
                System.out.println("Disconnected !");
                System.out.println("Reconnecting ..... ");
                reConnect();
            }

            @Override
            protected void onError(int errorCode, String error) {
                if (errorCode == CONNECTION_ERROR)
                    System.out.println("Connection error :- " + error);
            }
        };

        client.connect("127.0.0.1", 2150);
    }

    private void processObject(Object obj) {

    }

    public void disconnect() {
        if (client != null) {
            client.disconnect();
        }
    }

    public boolean isConnected() {
        if (client != null) {
            return client.isConnected();
        } else {
            return false;
        }
    }

    public void send(final Object obj) {

        client.postOutputStreamTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    out.writeObject(obj);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}

