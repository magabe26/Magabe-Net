package com.magabe.net.tests;

import com.magabe.net.MTcpServer;
import com.magabe.net.MTcpSocket;

import java.io.*;
import java.net.InetAddress;

public class MTcpServerTest {
    public static void main(String[] args) {
        MTcpServer server = new MTcpServer(2100, 5) {
            @Override
            protected void onClientConnected(MTcpSocket socket) {

                print("new connection from " + socket.toString());

                postOutputStreamTask(() -> {
                    try {
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        bufferedWriter.write(" HELLO CLIENT \n");
                        bufferedWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            }

            @Override
            protected void onClientDisconnected(InetAddress address, int port) {
                print(String.format("%s:%d disconnected", address, port));
            }


            @Override
            protected void onHandleInput(MTcpSocket socket) {
              /*  try {
                    System.out.print(inputStream.read());
                } catch (IOException e) {
                    //e.printStackTrace();
                }

               /* DataInputStream d = new DataInputStream(inputStream);
                try {
                    int i = d.readInt();
                    System.out.print(i);
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    try {
                        Object obj = objectInputStream.readObject();
                        processInput(obj, socket);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                }

            }

            private void processInput(Object obj, MTcpSocket socket) {
                if ((obj == null) || (socket == null)) return;

                String text;
                if (obj instanceof String) {
                    text = (String) obj;
                } else {
                    text = obj.toString();
                }
                System.out.print("data received := " + text+"\n");
            }

            @Override
            protected void onStarted() {
                super.onStarted();
                print("server started");
            }

            @Override
            protected void onStopped() {
                super.onStopped();
                print("server stoped");

            }

            private void print(String text) {
                System.out.println(text);
            }

            @Override
            protected void onError(int errorCode, String error) {
                if (errorCode == FAILED_TO_START_ERROR) {
                    print("server failed to start,Error: " + error);
                }
            }
        };

        server.start();
    }
}
