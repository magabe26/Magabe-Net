package com.magabe.net.tests.zmgame;

import com.magabe.net.MTcpServer;
import com.magabe.net.MTcpSocket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;

public class ZMGameAppletServer extends JApplet {
    private JTextArea textArea = new JTextArea();
    private JButton clearTextButton = new JButton("clear text");
    private JButton startServerButton = new JButton("start server");
    private JButton stopServerButton = new JButton("stop server");
    private MTcpServer server;
    boolean isStandAlone = false;

    ArrayList<MTcpSocket> sockests = new ArrayList<>();
    public static final String CONTROLLER_NAME = "PlayGameController";
    static final String FACILITATOR_NAME = "My_NAME";

    public static void main(String[] args) {
        ZMGameAppletServer server = new ZMGameAppletServer();
        server.isStandAlone = true;
        server.init();
    }

    @Override
    public void init() {
        super.init();

        textArea.setRows(getWidth());
        //textArea.setEditable(false);
        textArea.setAutoscrolls(true);
        textArea.setFocusable(true);
        JPanel content = new JPanel();
        JPanel btnContainer = new JPanel();
        btnContainer.setLayout(new BorderLayout());
        btnContainer.add(startServerButton, BorderLayout.WEST);
        btnContainer.add(clearTextButton, BorderLayout.CENTER);
        btnContainer.add(stopServerButton, BorderLayout.EAST);
        stopServerButton.setEnabled(false);

        content.setLayout(new BorderLayout());

        clearTextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               textArea.setText("");
            }
        });

        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                server.start();
            }
        });

        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                server.stop();
            }
        });

        if (isStandAlone) {
            JFrame jFrame = new JFrame("ZMGame Server");
            jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jFrame.setSize(620, 620);
            textArea.setColumns(jFrame.getHeight() - clearTextButton.getHeight());

            content.add(new JScrollPane(textArea), BorderLayout.CENTER);

            content.add(btnContainer, BorderLayout.NORTH);

            jFrame.setContentPane(content);
            jFrame.setVisible(true);
        } else {
            textArea.setColumns(getHeight() - clearTextButton.getHeight());
            content.add(new JScrollPane(textArea), BorderLayout.CENTER);

            content.add(btnContainer, BorderLayout.NORTH);

            setContentPane(content);
        }

        server = new MTcpServer(2100) {
            @Override
            protected void onClientConnected(MTcpSocket socket) {
                sockests.add(socket);
                print("New connection from " + socket.toString());
               //pauseAccepting();
            }

            @Override
            protected void onClientDisconnected(InetAddress address, int port) {
                print(String.format("Client;- %s:%d disconnected", address, port));
                //resumeAccepting();
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
            protected void onStarted() {
                super.onStarted();
                print("Server started");
                startServerButton.setEnabled(false);
                stopServerButton.setEnabled(true);
            }

            @Override
            protected void onStopped() {
                super.onStopped();
                print("Server stopped");
                startServerButton.setEnabled(true);
                stopServerButton.setEnabled(false);

            }

            @Override
            protected void onError(int errorCode, String error) {
                if (errorCode == FAILED_TO_START_ERROR) {
                    print("Server failed to start,Error: " + error);
                }
            }


        };


    }

    public void print(String text) {
        processObject(text);
    }

    private void processObject(Object obj) {
        String text;
        if (obj instanceof String) {
            text = (String) obj;
        } else {
            text = obj.toString();
        }

        acceptAnyPlayRequest(text);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                textArea.append(text + "\n");
            }
        });
    }


    public void acceptAnyPlayRequest(final String data) {
        final String ACTION_NAME = "PlayRequestAcceptedAction";
        final String PLAYER = "player";

        if (data.contains(CONTROLLER_NAME) && data.contains("PlayRequestReceivedAction")) {

            String acceptStr = "{\"Controller\" : " + "\"" + CONTROLLER_NAME + "\", " +
                    "\"Action\" : " + "\"" + ACTION_NAME + "\", " +
                    "\"Extras\" : { \"Facilitator\" : " + "\"" + FACILITATOR_NAME + "\"" + " }}";
            send(acceptStr);
        }

    }

    public void send(final Object obj) {

        for (MTcpSocket socket : sockests) {
            server.postOutputStreamTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(obj);
                        out.flush();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            });
        }
    }
}
