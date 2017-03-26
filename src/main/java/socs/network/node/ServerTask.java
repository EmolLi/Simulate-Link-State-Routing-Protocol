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
public class ServerTask extends NetworkTask{
	private Link port;


	public ServerTask(HashMap<String,Link> mapIpLink, Socket connection, RouterDescription localRouter, LinkStateDatabase db){
		super(mapIpLink, connection, localRouter, db);
	}

	public void run() {
		try {
			this.port = handleAcceptedConnection();
			if (port == null) return; //we may dont need the check here
			System.out.print(">> ");

			//connection set up successfully
			//now we listen for incoming packages
			while (true && this.port != null) {

				if (port.disconnected){
					if (disconnectLink(port)) break;
					// if we cannot disconnect the link at this point, we continue running, and try disconnect at another point
				}

				Packet packet = this.port.read();
				processPacket(packet);
				System.out.print(">> ");
			}

		}catch (Exception e){
			System.out.println(e);
			System.err.flush();
			if (!mapIpLink.containsKey(port.remote_router.simulatedIPAddress)) return;
			disconnectLink(port);
			System.out.println(">>");
			/**
			System.err.println("Connection to "+ port.remote_router.simulatedIPAddress +" is closed. ");
			mapIpLink.remove(port.remote_router.simulatedIPAddress);
			System.err.println("Deleted this link from ports.");**/
			return;
		}
	}

	private Link handleAcceptedConnection() throws Exception{
		Packet packetFromClient =  getInitPacket();
		RouterDescription remoteRouter = initRemoteRouterDescription(packetFromClient);

		Link link = this.createNewLink(remoteRouter.simulatedIPAddress, remoteRouter, packetFromClient.weight, connection);
		if (link == null){
			return null;
		}
		link.goesIN = true;
		return link;
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

	
	public void gotHelloMsg(Packet packet){
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
}
