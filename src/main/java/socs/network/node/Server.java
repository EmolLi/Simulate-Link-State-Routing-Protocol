package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import socs.network.message.Packet;




public class Server {
	private final ExecutorService threadPool;
	volatile private HashMap<String,Link> mapIpLink;
	private final RouterDescription localRouter;

	public Server(HashMap<String,Link> mapIpLink, RouterDescription localRouter){
		this.threadPool = Executors.newFixedThreadPool(5);
		//delete connection here because server has more than 1 connection.
		this.localRouter = localRouter;
		this.mapIpLink = mapIpLink;
	}

	public void startServer() throws IOException {
		final ServerSocket serverSocket = new ServerSocket(localRouter.processPortNumber);

		System.out.println("Server is running on port: "+localRouter.processPortNumber);

		Thread serverThread = new Thread(new Runnable() {
			public void run() {
				try {
					while(true) {
						Socket connection = serverSocket.accept();
						synchronized(mapIpLink){
							if (mapIpLink.size() < 4) {
								//accept the connection
								threadPool.submit(new ClientTask(mapIpLink, connection, localRouter));
							}
							else {
								connection.close();
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		serverThread.start();



	}



}
