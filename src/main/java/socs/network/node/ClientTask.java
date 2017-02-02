package socs.network.node;

import socs.network.message.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by emol on 2/2/17.
 */
public class ClientTask implements Runnable{
    private Socket connection;
    private final RouterDescription localRouter;
    private final HashMap<String,Link> mapIpLink;
    private Link port;

    public ClientTask(HashMap<String,Link> mapIpLink, Socket connection, RouterDescription localRouter){
        this.connection = connection;
        this.localRouter = localRouter;
        this.mapIpLink = mapIpLink;
    }

    public void run() {

        try {
            this.port = handleAcceptedConnection();
        } catch (Exception e){
            e.printStackTrace();
            return;
        }

        if (port == null) return; //we may dont need the check here

        //connection set up successfully
        //now we listen for incoming packages
        while(true){
            Packet packet = this.port.read();
            processPacket(packet);
        }


    }


    private Link handleAcceptedConnection() throws Exception{
        RouterDescription remoteRouter = initRemoteRouterDescription();
        //critical section
        Link link = new Link(this.localRouter, remoteRouter, connection);
        if(mapIpLink.size() < 4){
            this.mapIpLink.put(remoteRouter.simulatedIPAddress, link);

            System.out.println("Link created: "+remoteRouter.processPortNumber+" - "+remoteRouter.simulatedIPAddress);
            System.out.print(">> ");
            return link;
        }
        return null;

    }


    private RouterDescription initRemoteRouterDescription()throws IOException {
        RouterDescription remoteRouter = null;
        Packet packetFromClient;
        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

        try {
            packetFromClient = (Packet) in.readObject();
            if(this.localRouter.simulatedIPAddress.compareTo(packetFromClient.simulatedDstIP) == 0){
                remoteRouter = new RouterDescription(packetFromClient.simulatedSrcIP,packetFromClient.srcProcessPort);
            }
            else {
                throw new Exception("Not for us");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return remoteRouter;
    }


    //0 - HELLO, 1 - LinkState Update 2 - Attach
    private void processPacket(Packet packet){
        int packtype = packet.packetType;
        switch (packtype){
            case 0:
                gotHelloMsg(packet);
                break;
            case 1:
                //gotLSAUpdateMsg(packet);
                break;

            //set up connection
            case 2:
                //ignore it for now
                break;
                /**
                try {
                    this.port = handleAcceptedConnection();
                }catch (Exception e){
                    throw new Exception("Conncection set up error");
                }**/
            default:
                System.err.println("Some error in packet type.");
        }
    }


    private void gotHelloMsg(Packet packet){
        Link link = mapIpLink.get(packet.simulatedSrcIP);

        if(link.remote_router.status == RouterStatus.TWO_WAY){
            System.err.println("Already set to two way");
            return;
        }
        if(link.remote_router.status == RouterStatus.INIT){
            link.remote_router.status = RouterStatus.TWO_WAY;
            System.out.println("Set "+ link.remote_router.simulatedIPAddress +" to TWO WAY");
        }

        else {
            link.remote_router.status = RouterStatus.INIT;
            System.out.println("Set "+ link.remote_router.simulatedIPAddress +" to INIT");

            try {
                link.send(new Packet(link.local_router.simulatedIPAddress, link.remote_router.simulatedIPAddress, 0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
