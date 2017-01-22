package socs.network.util;

import socs.network.message.SOSPFPacket;

import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A router can also act as a client.
 * This class turns out to be unnecessary. DELETE LATER!
 */
public class Client {

    // later port should be changed to another parameter, like router object etc
    public void sendMsgToServer(int port, SOSPFPacket packet){
        try {
            Socket sock = new Socket("127.0.0.1", port);
            ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
            out.writeObject(packet);
            out.writeObject(packet);
            out.writeObject(packet);
            out.writeObject(packet);
            out.close();
            sock.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
