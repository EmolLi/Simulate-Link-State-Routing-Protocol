package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


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
		Thread clientSetup = new Thread( new ClientTask(mapIpLink, connectionToRemote, localRouterDescription, linkStateDatabase, weight, simulatedDstIP));
		clientSetup.start();
	}

	/**
	 * Starts HELLO protocol
	 */
	private void processStart() {
		for (Link connection : mapIpLink.values()){
			//we only start client.
			if (connection.isClient == false) continue;
			try {
				sendHELLO(connection);
			}catch (Exception e){
				System.err.println("Error in process start");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Start HELLO protocol
	 * @param connection
	 * @throws IOException
	 */
	//TODO: Make sure that we can't send HELLO twice
	private void sendHELLO(Link connection) throws IOException {
		connection.send(new Packet(connection.local_router.simulatedIPAddress, connection.remote_router.simulatedIPAddress, 0));
	}
	
	/**
	 * This is LSAUPDATE protocol used to send links to neighboring routers
	 * @param link
	 */
	

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
			if (neighbor.weight != 0)System.out.println(neighbor.remoteIP + "    distance: "+ neighbor.weight);
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
				else if(command.equals("db")){
					System.out.println(this.linkStateDatabase);
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
