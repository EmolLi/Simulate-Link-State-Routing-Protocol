package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import socs.network.message.Packet;

public class Server {
	private final HashMap<String,Link> mapIpLink;
	private Socket connection;
	private final RouterDescription localRouter;
	
	public Server(HashMap<String,Link> mapIpLink, RouterDescription localRouter){
		this.localRouter = localRouter;
		this.mapIpLink = mapIpLink;
	}

	public void startServer() throws IOException {
		final ServerSocket serverSocket = new ServerSocket(localRouter.processPortNumber);

		System.out.println("Server is running on port: "+localRouter.processPortNumber);
		
		Thread serverThread = new Thread(new Runnable() {
			public void run() {
				try {
					while(mapIpLink.size() < 4){
						connection = serverSocket.accept();
						handleAcceptedConnection();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		serverThread.start();		
	}
	
	private void handleAcceptedConnection() throws IOException {
		
		Packet packetFromClient;
		RouterDescription remoteRouter = initRemoteRouterDescription();
		//critical section
		Link link = new Link(this.localRouter, remoteRouter, connection);

		if(mapIpLink.size() < 4){
			this.mapIpLink.put(remoteRouter.simulatedIPAddress, link);
		}
		System.out.println("Link created: "+remoteRouter.processPortNumber+" - "+remoteRouter.simulatedIPAddress);
		System.out.print(">> ");;
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
}
