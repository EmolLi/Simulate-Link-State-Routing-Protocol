package socs.network.node;

import socs.network.message.SOSPFPacket;
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
import java.util.HashMap;


public class Router {
	private Socket connection;

	protected LinkStateDatabase lsd;

	RouterDescription receivingRouter = new RouterDescription();

	//assuming that all routers are with 4 ports
	private final HashMap<String,Link> mapIpLink = new HashMap<String,Link>();


	public Router(Configuration config) throws IOException {
		receivingRouter.simulatedIPAddress = config.getString("socs.network.router.ip");
		receivingRouter.processPortNumber = config.getShort("socs.network.router.port");
		lsd = new LinkStateDatabase(receivingRouter);
		startServer(this.mapIpLink);
	}
	
	public int getPortsSize(){
		return this.mapIpLink.size();
	}

	/* We need to be able to listen for incoming connections.
	 * Since we have in total four links, where each link is associated with a different port.
	 * We need our server to listen on four different sockets.
	 */
	private void startServer(final HashMap map) throws IOException {
		final ServerSocket serverSocket = new ServerSocket(receivingRouter.processPortNumber);

		System.out.println("Server is running on port:"+receivingRouter.processPortNumber);
		Thread serverThread = new Thread(new Runnable() {
			public void run() {
				try {
					while(map.size() < 4){
						connection = serverSocket.accept();
						handleAcceptedConnection(connection);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		serverThread.start();
	}


	//critical section because threads going to be modifying Link array 
	//Link link = new Link(rd);
	//start of a critical section since we want to allocate threads to different indecies
	private void handleAcceptedConnection(Socket connection) throws IOException {

		SOSPFPacket packetFromClient;
		RouterDescription remoteRouter = new RouterDescription();

		ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
		try {
			packetFromClient = (SOSPFPacket) in.readObject();
			if(this.receivingRouter.simulatedIPAddress.compareTo(packetFromClient.simulatedDstIP) == 0){
				remoteRouter.processPortNumber = packetFromClient.srcProcessPort;
				remoteRouter.simulatedIPAddress = packetFromClient.simulatedSrcIP;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}


		//critical section
		Link link = new Link(this.receivingRouter, remoteRouter);

		if(mapIpLink.size() < 4){
			this.mapIpLink.put(remoteRouter.simulatedIPAddress, link);
		}
		System.out.println("Link created: "+remoteRouter.processPortNumber+" - "+remoteRouter.simulatedIPAddress);
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
	private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
		try {
			if(!(this.mapIpLink.size() < 4)){
				return;
			}
			Socket sock = new Socket(processIP, processPort);
			System.out.println("Connected to server with:"+simulatedIP);

			SOSPFPacket data = new SOSPFPacket();
			data.simulatedDstIP = simulatedIP;
			data.simulatedSrcIP = this.receivingRouter.simulatedIPAddress;
			data.sospfType = 2;
			
			ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
			out.writeObject(data);
			RouterDescription remoteRouter = new RouterDescription();
			remoteRouter.simulatedIPAddress = simulatedIP;
			remoteRouter.processPortNumber = processPort;
			//we need to send router description of a connecting router
			Link link = new Link(this.receivingRouter, remoteRouter);
			if(this.mapIpLink.size() < 4){
				this.mapIpLink.put(simulatedIP, link);
			}
			

			sock.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("error connecting to "+processIP+":"+e);
		}
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

	}

	/**
	 * attach the link to the remote router, which is identified by the given simulated ip;
	 * to establish the connection via socket, you need to indentify the process IP and process Port;
	 * additionally, weight is the cost to transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {

	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

	}

	public void terminal() {
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
				} else {
					//invalid command
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
