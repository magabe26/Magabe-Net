package com.magabe.net;

import java.io.IOException;

public class MTcpSocketHelper {
    protected void forceCloseSocket(MTcpSocket socket){
        if(socket != null){
            try {
                socket.shutdownInput();
            } catch (IOException e) {
                // e.printStackTrace();
            }
            try {
                socket.shutdownOutput();
            } catch (IOException e1) {
                //e1.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
