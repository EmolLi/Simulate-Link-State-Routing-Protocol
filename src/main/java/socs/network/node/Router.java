package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;


public class Router {
	private Socket connection;
	private final Server server;

	private LinkStateDatabase linkStateDatabase;

	private RouterDescription localRouterDescription;

	//assuming that all routers are with 4 ports
	volatile private HashMap<String,Link> mapIpLink = new HashMap<String,Link>();

	public Router(Configuration config) throws IOException {
		localRouterDescription = new RouterDescription(config.getString("socs.network.router.ip"),config.getShort("socs.network.router.port"));
		linkStateDatabase = new LinkStateDatabase(localRouterDescription);
		this.server = new Server(mapIpLink,localRouterDescription, linkStateDatabase);
	}

	public int getPortsSize(){
		return this.mapIpLink.size();
	}



	/**
	 * output the shortest path to the given destination ip
	 * <p/>
	 * format: source ip address  -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP the ip adderss of the destination simulated router
	 */
	private void processDetect(String destinationIP) {

	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber the port number which the link attaches at
	 */
	private void processDisconnect(short portNumber) {

	}

	/**
	 * attach the link to the remote router, which is identified by the given simulated ip;
	 * to establish the connection via socket, you need to indentify the process IP and process Port;
	 * additionally, weight is the cost to transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, short processPort, String simulatedDstIP, short weight) throws Exception {

		if(this.localRouterDescription.simulatedIPAddress.equals(simulatedDstIP)){
			System.err.flush();
			System.err.print("Cannot connect to yourself!\n");
			return;
		}
		if(mapIpLink.containsKey(simulatedDstIP)){
			System.err.flush();
			System.err.print("Already connected to this router.\n");
			return;
		}

		if(!(this.mapIpLink.size() < 4)){
			System.err.flush();
			System.err.print("All ports are used.\n");
			return;
		}

		Socket connectionToRemote = new Socket(processIP, processPort);
		InetAddress local = connectionToRemote.getLocalAddress();

		try {
			//we need to pass the weight to the server, so it knows the weight of this link
			Packet packetToSend = Packet.AttachLinkRequest(this.localRouterDescription.simulatedIPAddress, simulatedDstIP, weight);

			ObjectOutputStream out = new ObjectOutputStream(connectionToRemote.getOutputStream());
			out.writeObject(packetToSend);
			RouterDescription remoteRouter = new RouterDescription(simulatedDstIP, processPort);

			//we need to send router description of a connecting router
			Link link = new Link(this.localRouterDescription, remoteRouter, connectionToRemote, weight);
			link.isClient = true;


			link.send(packetToSend);

			synchronized(mapIpLink){
				if(this.mapIpLink.size() < 4){ //you may want to check again because of competition between different
					// threads.
					//add critical section
					this.mapIpLink.put(simulatedDstIP, link);
				}
				else{
					//dont throw exception
					System.err.flush();
					System.err.print("All ports are used.\n");
					return;
				}
			}

		} catch (Exception e){
			System.err.flush();
			System.err.print("Connection rejected by remote router.\n");
			return;
		}




		System.out.println("Connected to server with:" + simulatedDstIP);
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

		for (Link link : mapIpLink.values()){
			//we only start client.
			if (link.isClient == false) continue;
			try {
				link.send(new Packet(link.local_router.simulatedIPAddress, link.remote_router.simulatedIPAddress, 0));

				Packet packet = link.read();

				if(packet.packetType == 0){
					System.out.println("received HELLO from "+ packet.simulatedSrcIP);
					link.remote_router.status = RouterStatus.TWO_WAY;
					
					linkStateDatabase.newLSA(link);//we insert new neighbor into our database
					broadcastToNeighbors(link, linkStateDatabase.getLSA(localRouterDescription.simulatedIPAddress));
					System.out.println("Set "+ link.remote_router.simulatedIPAddress + "to TWO WAY");
				}
				else {
					System.err.println("Expecting packet HELLO");
				}

				link.send(new Packet(link.local_router.simulatedIPAddress, link.remote_router.simulatedIPAddress, 0));

			}catch (Exception e){
				System.err.println("Error in process start");
				e.printStackTrace();
			}


		}

		//LSA database update

		/**
		LSA lsa = new LSA(this.localRouterDescription.simulatedIPAddress, lsd.getNextLSASeqNum());
		for (Link link : mapIpLink.values()){
			lsa.links.add(link.linkDescription);
		}

		try {
			//update local lsa
			lsd.updateLSA(lsa);

			//broadcast LSAUPDATE
		}catch (Exception e){
			System.err.println("NO NEED To UPDATE: " + lsa.toString());
		}

		//boardcast LSAUPDATE
		 **/
	}


	private void broadcastToNeighbors(Link link_to_ignore, LSA lsa) {
		LSA neighbors = linkStateDatabase.getLSA(localRouterDescription.simulatedIPAddress);

		System.out.println(neighbors);
		for(LinkDescription neighbor : neighbors.links){
			if(neighbor.remoteRouter.compareTo(localRouterDescription.simulatedIPAddress) == 0){
				continue; //we don't want to sent to ourselves
			}

			Link link_of_neighbor = mapIpLink.get(neighbor.remoteRouter);
			if(link_to_ignore != link_of_neighbor){
				ArrayList<LSA> linkStateAdvertisements = new ArrayList<LSA>();//in case we need array
				linkStateAdvertisements.add(lsa);
				Packet packet = Packet.LSAUPDATE(localRouterDescription.simulatedIPAddress, link_of_neighbor.remote_router.simulatedIPAddress,linkStateAdvertisements);
				System.out.println("Send LSAUPDATE to: "+link_of_neighbor.remote_router.simulatedIPAddress);
				try {
					link_of_neighbor.send(packet);
				} catch (IOException e) {
					System.err.println("Mistake in sending LSAUPDATE");
					e.printStackTrace();
				}
			}
			else{
				System.out.println("Don't send LSAUPDATE to: "+link_to_ignore.remote_router.simulatedIPAddress);
			}
		}
	}

	/**
	 * attach the link to the remote router, which is identified by the given simulated ip;
	 * to establish the connection via socket, you need to indentify the process IP and process Port;
	 * additionally, weight is the cost to transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

		//TODO: check if we have run start before, if not return

		//like attach, attach to the remote router if we can. (create the link)

		//like start, but we only send hello on this one link (two way communication on this link)

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		for (LinkDescription neighbor : linkStateDatabase.getNeighbors()){
			//our local router is also in the neighbor list, and it has weight 0
			if (neighbor.weight != 0)System.out.println(neighbor.remoteRouter + "    distance: "+ neighbor.weight);
		}
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

	}




	private void terminal() {
		/*		try {
			//startServer(this.mapIpLink);
		} catch (IOException e1) {
			System.out.println("Server Error");
			e1.printStackTrace();
		}*/
		try {
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();

			while (true) {
				if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
				} else if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
							cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("start")) {
					processStart();
				} else if (command.equals("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
							cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					//output neighbors
					processNeighbors();
				} else if(command.equals("shutdown")){
					//invalid command
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
			System.out.println("Shuting down");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startRouter(){
		try {
			this.server.startServer();
		} catch (IOException e) {
			System.err.println("Error with SERVER");
			e.printStackTrace();
		}		
		this.terminal();
	}

}
