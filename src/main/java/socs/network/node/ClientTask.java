package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


public class ClientTask implements Runnable {
    private final RouterDescription localRouter;
    volatile private HashMap<String, Link> mapIpLink;
    private Link port;
    volatile LinkStateDatabase linkStateDatabase;

    public ClientTask(HashMap<String, Link> mapIpLink, Socket connection, RouterDescription localRouter, LinkStateDatabase db, short weight, String simulatedDstIP) {
        this.localRouter = localRouter;
        this.mapIpLink = mapIpLink;
        this.linkStateDatabase = db;
        this.port = this.attachToRemote(simulatedDstIP, weight, connection, (short) connection.getPort());
        if (port == null) {
            System.out.println("Connection failed");
        }
    }

    public void run() {
        try {
            while (true) {
                Packet packet = this.port.read();
                processPacket(packet);
                System.out.print(">>");
            }
        } catch (Exception e) {
            System.out.println(e);
            System.err.flush();
            if (!mapIpLink.containsKey(port.remote_router.simulatedIPAddress)) return;
            System.err.println("Connection to " + port.remote_router.simulatedIPAddress + " is closed. ");
            mapIpLink.remove(port.remote_router.simulatedIPAddress);
            System.err.println("Deleted this link from ports.");
            return;
        }
    }

    private void processPacket(Packet packet) {
        int packtype = packet.packetType;
        switch (packtype) {
            case 0:
                gotHelloMsg(packet);
                break;
            case 1:
                gotLSAUpdateMsg(packet);
                break;
            case 2:
                //attach
                break;
            default:
                System.err.println("Some error in packet type.");
        }
    }

    /**
     * This is Hello Protocol on Server Side
     *
     * @param packet
     */
    private void gotHelloMsg(Packet packet) {
        System.out.println("received HELLO from " + packet.simulatedSrcIP);
        port.remote_router.status = RouterStatus.TWO_WAY;
        System.out.println("Set " + port.remote_router.simulatedIPAddress + " to TWO WAY");
        try {
            port.send(new Packet(port.local_router.simulatedIPAddress, port.remote_router.simulatedIPAddress, 0));
        } catch (IOException e) {
            System.err.println("error in gotHelloMsg in ClientTask");
            e.printStackTrace();
        }
        performLSAUPDATE(port);
    }


    private synchronized void performLSAUPDATE(Link link) {
        linkStateDatabase.addNewLinkToDB(link);//we insert new neighbor into our database
        broadcastToNeighbors();
    }


    /**
     * We broadcast to every neighbor because after running HELLO, it is guaranteed that there is no neighbor that has this data.
     *
     * @param lsa
     */
    private void broadcastToNeighbors() {
        String srcIp = localRouter.simulatedIPAddress;
        LSA neighbors = linkStateDatabase.getLSA(srcIp);

        for (LinkDescription neighbor : neighbors.links) {
            if (neighbor.remoteIP.compareTo(srcIp) == 0) {
                continue; //we don't want to sent to ourselves
            }

            Link connectionToNeighbor = mapIpLink.get(neighbor.remoteIP);
            ArrayList<LSA> linkStateAdvertisements = this.linkStateDatabase.getLSAs();//maybe it is used for buffering LSAs

            String destinationIp = connectionToNeighbor.remote_router.simulatedIPAddress;

            Packet lsaUpdate = Packet.LSAUPDATE(srcIp, destinationIp, linkStateAdvertisements);

            try {
                System.out.println("send LSAUPDATE to: " + destinationIp);
                connectionToNeighbor.send(lsaUpdate);
            } catch (IOException e) {
                System.err.println("Mistake in sending LSAUPDATE");
                e.printStackTrace();
            }
        }
    }


    /**
     * We check if we already have this LSA. If we don't then we forward to all our neighbors.
     *
     * @param packet
     */
    private void gotLSAUpdateMsg(Packet packet) {
        System.out.println("received LSUPDATE from " + packet.simulatedSrcIP);
        LinkStateDatabase db = this.linkStateDatabase;

        for (LSA lsa : packet.lsaArray) {
            if (isAlreadyInDb(db, lsa)) {
                continue;//since we only have one lsa per array
            } else {
                try {
                    if (!db.updateLSA(lsa)) return;
                } catch (Exception e) {
                    System.err.println("could not update LinkStateDatabase");
                    e.printStackTrace();
                }
                Link linkOverWhichWeReceivedLSA = mapIpLink.get(packet.simulatedSrcIP);
                forwardToNeighbors(linkOverWhichWeReceivedLSA, lsa);
            }
        }
    }

    private void forwardToNeighbors(Link linkOverWhichWeReceived, LSA lsa) {
        LSA neighbors = linkStateDatabase.getLSA(localRouter.simulatedIPAddress);

        for (LinkDescription neighbor : neighbors.links) {
            if (this.localRouter.simulatedIPAddress.compareTo(neighbor.remoteIP) == 0) {
                continue; //don't forward to yourself
            }


            Link neigborConnection = mapIpLink.get(neighbor.remoteIP);
            if (linkOverWhichWeReceived != neigborConnection) {
                if (neighbor.remoteIP.equals("192.168.3.1")){
                    //continue;
                }
                System.out.println("Forwarding to: " + neighbor.remoteIP);
                sendLSAUPDATEPacket(lsa, neigborConnection);
            }
        }
    }

    private void sendLSAUPDATEPacket(LSA linkStateAdvertisement, Link link_of_neighbor) {
        ArrayList<LSA> linkStateAdvertisements = new ArrayList<LSA>();//in case we need array
        linkStateAdvertisements.add(linkStateAdvertisement);
        Packet packet = Packet.LSAUPDATE(localRouter.simulatedIPAddress, link_of_neighbor.remote_router.simulatedIPAddress, linkStateAdvertisements);
        try {
            link_of_neighbor.send(packet);
        } catch (IOException e) {
            System.err.println("Mistake in forwarding LSAUPDATE");
            e.printStackTrace();
        }
    }

    private boolean isAlreadyInDb(LinkStateDatabase db, LSA lsa) {
        if (db.hasEntryFor(lsa.routerSimulatedIP)) {
            return lsa.lsaSeqNumber < db.getLSA(lsa.routerSimulatedIP).lsaSeqNumber;
        } else {
            return false;
        }
    }


    private Link attachToRemote(String simulatedDstIP, int weight, Socket connectionToRemote, short processPort) {
        try {
            //we need to pass the weight to the server, so it knows the weight of this link
            //we don't need it since we are going to get weight after running hello
            Packet attachRequest = Packet.AttachLinkRequest(this.localRouter.simulatedIPAddress, simulatedDstIP, weight);

            ObjectOutputStream out = new ObjectOutputStream(connectionToRemote.getOutputStream());
            out.writeObject(attachRequest);
            RouterDescription remoteRouter = new RouterDescription(simulatedDstIP, processPort);

            //we need to send router description of a connecting router
            Link link = new Link(this.localRouter, remoteRouter, connectionToRemote, weight);
            link.isClient = true;//why do we use this?


            link.send(attachRequest);

            //check if we can connect to more servers
            synchronized (mapIpLink) {
                if (this.mapIpLink.size() < 4) {
                    this.mapIpLink.put(simulatedDstIP, link);
                } else {
                    System.err.flush();
                    System.err.print("We are already connected to three servers.\n");
                    return null;
                }
            }
            return link;

        } catch (Exception e) {
            System.err.flush();
            System.err.print("Connection rejected by remote router.\n");
            return null;
        }
    }
}
