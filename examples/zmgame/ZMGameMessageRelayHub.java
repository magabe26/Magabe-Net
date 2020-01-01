package com.magabe.net.tests.zmgame;

import com.magabe.net.MTcpClient;
import com.magabe.net.MTcpServer;
import com.magabe.net.MTcpSocket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

/*
* foeward por fies
*
adb forward tcp:2100 tcp:2100

adb forward --list
emulator-5554 tcp:2100 tcp:2100
*
* */

/*NOTE FOR THIS TO WORK MAKE SURE ,U HAVE THREE CONNECTION
-----ONE        FROM PHONE APP TO SERVER-HUB
------ TWO FORM  FROM EMULATOR APP TO SERVER-HUB  ie app client connected :- 192.168.43.158
----THE LAST FROM HUB CLIENT TO EMULATOR APP ie connected to emulator app:-

*/
public class ZMGameMessageRelayHub {

    class HubServer extends MTcpServer {
        MTcpSocket phoneSocket = null;
        final String emulatorClientHostAddress = "192.168.43.158";

        int count = 0;

        public HubServer(int port) throws IllegalArgumentException {
            super(port);
        }

        boolean isEmulatorClientApp(String hostAddr) {
            return hostAddr.equals(emulatorClientHostAddress);
        }

        @Override
        protected void onClientConnected(MTcpSocket socket) {
            System.out.println("app client connected :- " + socket.getInetAddress().getHostAddress());
            if (!isEmulatorClientApp(socket.getInetAddress().getHostAddress())) {
                phoneSocket = socket;
            }
           /* count++;
            if (count == 2) {
                pauseAccepting();
                System.out.println("All parties connected , pauseAccepting :- ");

            }*/
        }

        @Override
        protected void onClientDisconnected(InetAddress address, int port) {
            System.out.println("Phone app disconnected ... ");

           /* count--;
            if (count < 2) {
                resumeAccepting();
                System.out.println("one parties disconnectes , resumeAccepting :- ");
            }*/
        }

        @Override
        protected void onStarted() {
            super.onStarted();
            System.out.println("ZMGameMessageRelayHub  started | ");
        }

        @Override
        protected void onStopped() {
            super.onStopped();
            System.out.println("ZMGameMessageRelayHub  stoped | ");
        }

        @Override
        protected void onHandleInput(MTcpSocket socket) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                try {
                    Object obj = objectInputStream.readObject();
                    if (isEmulatorClientApp(socket.getInetAddress().getHostAddress())) {
                        processMessageFromEmulator(obj, socket);
                    } else {
                        processMessageFromphone(obj, socket);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }

        @Override
        protected void onError(int errorCode, String error) {

        }

        public void sendToPhone(Object obj) {
            if (phoneSocket == null) return;
            postOutputStreamTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectOutputStream out = new ObjectOutputStream(phoneSocket.getOutputStream());
                        out.writeObject(obj);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    class HubClient extends MTcpClient {
        @Override
        protected void onConnected() {
            System.out.println("connected to emulator app:- ");
        }

        @Override
        protected void onDisconnected() {
            System.out.println("disconnected from emulator app:-,reconnecting ... ");
            reConnect();
        }

        @Override
        protected void onHandleInput(MTcpSocket socket) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                try {
                    Object obj = objectInputStream.readObject();
                    processMessageFromEmulator(obj, socket);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }

        @Override
        protected void onError(int errorCode, String error) {

        }

        public void sendToEmulator(Object obj) {

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

    HubClient hubClient = new HubClient();
    HubServer hubServer = new HubServer(2600);

    private void processMessageFromphone(Object obj, MTcpSocket socket) {
        if ((obj == null) || (socket == null)) return;
        System.out.println("data received from phone app,sending it to emulator app ..\n" + obj.toString());

        hubClient.sendToEmulator(obj);
    }

    private void processMessageFromEmulator(Object obj, MTcpSocket socket) {
        if ((obj == null) || (socket == null)) return;
        System.out.println("data received from emulator app,sending it to phone app .. \n" + obj.toString());

        hubServer.sendToPhone(obj);
    }


    public ZMGameMessageRelayHub() {
        hubServer.start();
        hubClient.connect("127.0.0.1", 2100);
    }

    public static void main(String[] args) {
        new ZMGameMessageRelayHub();
    }
}
