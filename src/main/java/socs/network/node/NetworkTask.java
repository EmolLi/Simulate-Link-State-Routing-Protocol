package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


public abstract class NetworkTask implements Runnable{
	protected Socket connection;
	protected final RouterDescription localRouter;
	protected final HashMap<String,Link> mapIpLink;
	volatile LinkStateDatabase linkStateDatabase;

	public NetworkTask(HashMap<String,Link> mapIpLink, Socket connection, RouterDescription localRouter, LinkStateDatabase db){
		this.connection = connection;
		this.localRouter = localRouter;
		this.mapIpLink = mapIpLink;
		this.linkStateDatabase = db;
	}

	//0 - HELLO, 1 - LinkState Update 2 - Attach
	public void processPacket(Packet packet){
		//in case packet stuck in loop.
		/**
		if (packet.TTL <= 0){
			//drop packet
			return;
		}**/

		int packtype = packet.packetType;
		switch (packtype){
		case 0:
			gotHelloMsg(packet);
			break;
		case 1:
			gotLSAUpdateMsg(packet);
			break;
		case 2:
			break;
		default:
			System.err.println("Some error in packet type.");
		}
	}

	
	public abstract void gotHelloMsg(Packet packet);

	/**
	 * We check if we already have this LSA. If we don't then we forward to all our neighbors.
	 * @param packet
	 */
    private void gotLSAUpdateMsg(Packet packet) {
        System.out.println("received LSUPDATE from " + packet.simulatedSrcIP);
        LinkStateDatabase db = this.linkStateDatabase;

        for (LSA lsa : packet.lsaArray) {
        /*    if (isAlreadyInDb(db, lsa)) {
                continue;//since we only have one lsa per array
            } else {*/
                try {
                	boolean newLSA = db.updateLSA(lsa); 
                    if (!newLSA){
                    	continue;
                    }
                    else{
                        Link linkOverWhichWeReceivedLSA = mapIpLink.get(packet.simulatedSrcIP);
                        forwardToNeighbors(linkOverWhichWeReceivedLSA, lsa);
                    }
                } catch (Exception e) {
                    System.err.println("could not update LinkStateDatabase");
                    e.printStackTrace();
                }
        }
    }
	
	
	private void forwardToNeighbors(Link linkOverWhichWeReceived, LSA lsa) {
		LSA neighbors = linkStateDatabase.getLSA(localRouter.simulatedIPAddress);
	
		for(LinkDescription neighbor : neighbors.links){
			if(this.localRouter.simulatedIPAddress.compareTo(neighbor.remoteIP) == 0){
				continue; //don't forward to yourself
			}
			
			Link neigborConnection = mapIpLink.get(neighbor.remoteIP);
			if(linkOverWhichWeReceived != neigborConnection){
				if (neighbor.remoteIP.equals("192.168.3.1")){
					//continue;
				}
				System.out.println("Forwarding to: "+neighbor.remoteIP);
				sendLSAUPDATEPacket(lsa, neigborConnection);
			}
		}
	}

	private void sendLSAUPDATEPacket(LSA linkStateAdvertisement, Link link_of_neighbor) {
		ArrayList<LSA> linkStateAdvertisements = new ArrayList<LSA>();//in case we need array
		linkStateAdvertisements.add(linkStateAdvertisement);
		Packet packet = Packet.LSAUPDATE(localRouter.simulatedIPAddress, link_of_neighbor.remote_router.simulatedIPAddress,linkStateAdvertisements);
		try {
			link_of_neighbor.send(packet);
		} catch (IOException e) {
			System.err.println("Mistake in forwarding LSAUPDATE");
			e.printStackTrace();
		}
	}
	
	private boolean isAlreadyInDb(LinkStateDatabase db, LSA lsa) {
		if(db.hasEntryFor(lsa.routerSimulatedIP)){
			return lsa.lsaSeqNumber < db.getLSA(lsa.routerSimulatedIP).lsaSeqNumber;
		}
		else{
			return false;
		}
	}
	
	
	public synchronized void performLSAUPDATE(Link link) {
		linkStateDatabase.addNewLinkToDB(link);//we insert new neighbor into our database
		broadcastLSAUPDATE();
	}

	/**
	 * After finishing HELLO on client. We are sending LSAUPDATE to every neighbor.
	 */
	public void broadcastLSAUPDATE() {
			LSA neighborsOfThisServer = linkStateDatabase.getLSA(localRouter.simulatedIPAddress);
			ArrayList<LSA> linkStateAdvertisements = this.linkStateDatabase.getLSAs();
			
			for(LinkDescription neighbor : neighborsOfThisServer.links){
				if(neighbor.remoteIP.compareTo(localRouter.simulatedIPAddress) == 0){
					continue; //we don't want to sent to ourselves
				}
				sendLSAUPDATE(neighbor, linkStateAdvertisements);
			}		
	}
	
	private void sendLSAUPDATE(LinkDescription neighbor, ArrayList<LSA> linkStateAdvertisements){

		Link connection = mapIpLink.get(neighbor.remoteIP);
		Packet packet = Packet.LSAUPDATE(localRouter.simulatedIPAddress, connection.remote_router.simulatedIPAddress,linkStateAdvertisements);
		
		System.out.println("send LSAUPDATE to: "+connection.remote_router.simulatedIPAddress);
		try {
			connection.send(packet);
		} catch (IOException e) {
			System.err.println("Mistake in sending LSAUPDATE");
			e.printStackTrace();
		}
	}

}