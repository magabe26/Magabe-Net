package com.magabe.net.tests;

import com.magabe.net.MTcpClient;
import com.magabe.net.MTcpSocket;

import java.io.*;
import java.net.InetAddress;

public class MTcpClientTest {
    public static void main(String[] args) {

        MTcpClient client = new MTcpClient() {
            @Override
            protected void onHandleInput(MTcpSocket socket) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                try {
                    String line = in.readLine();
                    System.out.println("data received := " + line);
                } catch (IOException e) {
                    // e.printStackTrace();
                }
            }

            @Override
            protected void onConnected() {
                System.out.print("connected  ");

                postOutputStreamTask(() -> {
                    try {
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(getOutputStream()));
                        bufferedWriter.write(" HELLO SERVER \n");
                        bufferedWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

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
        };

        client.connect("127.0.0.1", 2100);
    }

}
