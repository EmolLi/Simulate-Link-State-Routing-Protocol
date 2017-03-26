package socs.network.node;

import socs.network.message.Packet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;


public class ClientTask extends NetworkTask {
    private Link port;

    public ClientTask(HashMap<String, Link> mapIpLink, Socket connection, RouterDescription localRouter, LinkStateDatabase db, short weight, String simulatedDstIP) {
    	super(mapIpLink, connection, localRouter, db);
    	
        this.port = this.attachToRemote(simulatedDstIP, weight, connection, (short) connection.getPort());
    }

    public void run() {
        try {
            while (true && port != null) {
                if (port.disconnected){
                    if (disconnectLink(port)) break;
                    // if we cannot disconnect the link at this point, we continue running, and try disconnect at another point
                }
                Packet packet = this.port.read();

                processPacket(packet);
                System.out.print(">>");
            }
        } catch (Exception e) {
            System.out.println(e);
            System.err.flush();
            if (!mapIpLink.containsKey(port.remote_router.simulatedIPAddress)) return;

            // TODO: change this part later
            disconnectLink(port);
            System.out.println(">>");
            /**
            System.err.println("Connection to " + port.remote_router.simulatedIPAddress + " is closed. ");
            mapIpLink.remove(port.remote_router.simulatedIPAddress);
            System.err.println("Deleted this link from ports.");**/
            return;
        }
    }

    /**
     * This is Hello Protocol on Server Side
     *
     * @param packet
     */
    public void gotHelloMsg(Packet packet) {
        System.out.println("received HELLO from " + packet.simulatedSrcIP);
        port.remote_router.status = RouterStatus.TWO_WAY;
        System.out.println("Set " + port.remote_router.simulatedIPAddress + " to TWO WAY");
        try {
            port.send(new Packet(port.local_router.simulatedIPAddress, port.remote_router.simulatedIPAddress, 0));//last hello message from client
        } catch (IOException e) {
            System.err.println("error in gotHelloMsg in ClientTask");
            e.printStackTrace();
        }
        performLSAUPDATE(port);
    }


    private Link attachToRemote(String simulatedDstIP, int weight, Socket connection, short processPort) {
        try {
            //we need to pass the weight to the server, so it knows the weight of this link
            //we don't need it since we are going to get weight after running hello
            Packet attachRequest = Packet.AttachLinkRequest(this.localRouter.simulatedIPAddress, simulatedDstIP, weight);

            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            out.writeObject(attachRequest);
            RouterDescription remoteRouter = new RouterDescription(simulatedDstIP, processPort);

            //we need to send router description of a connecting router
            Link link = this.createNewLink(simulatedDstIP, remoteRouter, weight, connection);
            if (link == null){
                return null;
            }
            link.goesIN = false;
            link.send(attachRequest);

            return link;

        } catch (Exception e) {
            System.err.flush();
            System.err.print("Connection rejected by remote router.\n");
            return null;
        }
    }
}
