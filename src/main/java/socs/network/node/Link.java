package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.LinkDescription;
import socs.network.message.Packet;

public class Link {

	RouterDescription local_router;
	RouterDescription remote_router;
	boolean goesIN; //only client side start the router!
	int portNum;
	int weight;
	final Socket connection;
	ObjectOutputStream out;
	ObjectInputStream in;

	LinkDescription linkDescription;
	boolean disconnected;	// if we try to disconnect a link, we set disconnected to boolean, and then we will try to close the socket if there is a opportunity
    boolean linkClosed; // we already closed this link

	/**
	 *
	 * @param local_router
	 * @param remote_router
	 * @param connection
	 * @throws IOException
	 */
	public Link(RouterDescription local_router, RouterDescription remote_router, Socket connection, int weight, int portNum) throws IOException {
		this.local_router = local_router;
		this.remote_router = remote_router;
		this.portNum = portNum;
		this.weight = weight;
        this.connection = connection;
		this.out = new ObjectOutputStream(connection.getOutputStream());
		this.in = new ObjectInputStream(connection.getInputStream());
		this.linkDescription = new LinkDescription(remote_router.simulatedIPAddress, connection.getLocalPort(), weight);
		this.disconnected = false;
		this.linkClosed = false;
	}


    /**
     * try close this link
     * the link may not be closed immediately
     */
	public boolean close(){
		disconnected = true;
		try {
		    in.close();
		    out.close();
		    connection.close();
		    System.out.println("success");
		    linkClosed = true;
		    return true;
		}catch (Exception e){
		    System.out.println("...closing connection...");
		    return false;
        }
	}

	public synchronized void send(Packet packet) throws IOException{
	    try {

			if (this.connection == null){
				System.out.print("connection null");
			}
			out.writeObject(packet);
		}catch (Exception e){
	    	System.err.flush();
	    	System.err.println("Connection closed by remote router: "+ this.remote_router.simulatedIPAddress);
	    	throw new IOException("connection closed");
		}
	}


	public Packet read()throws IOException{
		Packet packetFromClient=null;
		try {
			packetFromClient = (Packet) in.readObject();

		} catch (Exception e) {
			System.err.flush();
			System.err.println("Connection closed by remote router: "+ this.remote_router.simulatedIPAddress);
			throw new IOException("connection closed");
		}
		return packetFromClient;
	}
}