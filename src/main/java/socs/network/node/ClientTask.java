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
            if (port == null) return; //we may dont need the check here
            System.out.print(">> ");

            //connection set up successfully
            //now we listen for incoming packages
            while (true) {
                Packet packet = this.port.read();
                processPacket(packet);
            }

        }catch (Exception e){
                //remote router is closed.
                System.err.flush();
                if (!mapIpLink.containsKey(port.remote_router.simulatedIPAddress)) return;
                System.err.println("Connection to "+ port.remote_router.simulatedIPAddress +" is closed. ");
                mapIpLink.remove(port.remote_router.simulatedIPAddress);
                System.err.println("Deleted this link from ports.");
                return;
        }
    }




    private Link handleAcceptedConnection() throws Exception{
        Packet packetFromClient =  getInitPacket();

        RouterDescription remoteRouter = initRemoteRouterDescription(packetFromClient);
        //critical section
        Link link = new Link(this.localRouter, remoteRouter, connection, packetFromClient.weight);
        link.isClient = false;
        if(mapIpLink.size() < 4){
            this.mapIpLink.put(remoteRouter.simulatedIPAddress, link);

            System.out.println("Link created: "+remoteRouter.processPortNumber+" - "+remoteRouter.simulatedIPAddress);
            //System.out.print(">> ");
            return link;
        }
        return null;

    }


    /**
     * This method reads the first packet the client send (the packet to set up the link)
     * The packet serves two purpose:
     *  1. it carries the link weight
     *  2. it carries the simulated address of the destination router, so we have check whether this packet is for us, or user may have made a typo
     * @return {Packet} initial packet
     * @throws Exception
     */
    private Packet getInitPacket() throws Exception{
        Packet packetFromClient = null;
        try{
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            packetFromClient = (Packet) in.readObject();
        } catch (Exception e){
            e.printStackTrace();
        }
        if (packetFromClient == null) throw new Exception("didn't get init packet");
        return packetFromClient;
    }


    private RouterDescription initRemoteRouterDescription(Packet packetFromClient)throws Exception {
        RouterDescription remoteRouter = null;

        //take the reading packet part out of the initRemoteROuterDescription method because we also need to read link weight from the packet --> see method getInitpacket

        if(this.localRouter.simulatedIPAddress.compareTo(packetFromClient.simulatedDstIP) == 0){
            remoteRouter = new RouterDescription(packetFromClient.simulatedSrcIP,packetFromClient.srcProcessPort);
        }
        else {
            throw new Exception("Not for us");
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
        System.out.println("received HELLO from "+ packet.simulatedSrcIP);
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
