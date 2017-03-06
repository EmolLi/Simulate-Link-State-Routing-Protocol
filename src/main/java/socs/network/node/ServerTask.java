package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by emol on 2/2/17.
 */
public class ServerTask implements Runnable{
	private Socket connection;
	private final RouterDescription localRouter;
	private final HashMap<String,Link> mapIpLink;
	private Link port;
	volatile LinkStateDatabase linkStateDatabase;

	public ServerTask(HashMap<String,Link> mapIpLink, Socket connection, RouterDescription localRouter, LinkStateDatabase db){
		this.connection = connection;
		this.localRouter = localRouter;
		this.mapIpLink = mapIpLink;
		this.linkStateDatabase = db;
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
				System.out.print(">> ");
			}

		}catch (Exception e){
			System.out.println(e);
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
		Link link = new Link(this.localRouter, remoteRouter, connection, packetFromClient.weight);
		link.isClient = false;
		synchronized(mapIpLink){
			if(mapIpLink.size() < 4){
				this.mapIpLink.put(remoteRouter.simulatedIPAddress, link);
				System.out.println("Link created: "+remoteRouter.processPortNumber+" - "+remoteRouter.simulatedIPAddress);//process port number is always 0
				return link;
			}
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


	/**
	 * We check if we already have this LSA. If we don't then we forward to all our neighbors.
	 * @param packet
	 */
	private void gotLSAUpdateMsg(Packet packet) {
		System.out.println("received LSUPDATE from "+packet.simulatedSrcIP);
		LinkStateDatabase db = this.linkStateDatabase;
		
		System.out.println(packet.print());
		for(LSA lsa : packet.lsaArray){
			System.out.println(lsa);
			if(isAlreadyInDb(db, lsa)){
				continue;
			}
			else{
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
	
	/**
	 * This is Hello Protocol on Server Side
	 * @param packet
	 */
	private void gotHelloMsg(Packet packet){
		System.out.println("received HELLO from "+ packet.simulatedSrcIP);
		Link connection = mapIpLink.get(packet.simulatedSrcIP);

		if(connection.remote_router.status == RouterStatus.TWO_WAY){
			System.err.println("Already set to two way");
			return;
		}
		
		if(connection.remote_router.status == RouterStatus.INIT){
			connection.remote_router.status = RouterStatus.TWO_WAY;
			System.out.println("Set "+ connection.remote_router.simulatedIPAddress +" to TWO WAY");
			System.out.println("add new connection to db");
			performLSAUPDATE(connection);
		}
		else {
			connection.remote_router.status = RouterStatus.INIT;
			System.out.println("Set "+ connection.remote_router.simulatedIPAddress +" to INIT");

			try {
				connection.send(new Packet(connection.local_router.simulatedIPAddress, connection.remote_router.simulatedIPAddress, 0));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private synchronized void performLSAUPDATE(Link link) {
		linkStateDatabase.addNewLinkToDB(link);//we insert new neighbor into our database
		broadcastLSAUPDATE();
	}

	/**
	 * After finishing HELLO on client. We are sending LSAUPDATE to every neighbor.
	 */
	private void broadcastLSAUPDATE() {
			LSA neighborsOfThisServer = linkStateDatabase.getLSA(localRouter.simulatedIPAddress);

			for(LinkDescription neighbor : neighborsOfThisServer.links){
				if(neighbor.remoteIP.compareTo(localRouter.simulatedIPAddress) == 0){
					continue; //we don't want to sent to ourselves
				}

				Link connection = mapIpLink.get(neighbor.remoteIP);
				
				ArrayList<LSA> linkStateAdvertisements = this.linkStateDatabase.getLSAs();
				linkStateAdvertisements.add(neighborsOfThisServer);//we send all neighbors of local router
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

}
