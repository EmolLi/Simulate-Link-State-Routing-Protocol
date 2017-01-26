package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.Packet;

public class Link {

	RouterDescription local_router;
	RouterDescription remote_router;
	Socket connection;
	ObjectOutputStream out;
	ObjectInputStream in;

	public Link(RouterDescription local_router, RouterDescription remote_router, Socket connection) throws IOException {
		this.local_router = local_router;
		this.remote_router = remote_router;
		this.out = new ObjectOutputStream(connection.getOutputStream());
		this.in = new ObjectInputStream(connection.getInputStream());
	}
	/*
	public Link(RouterDescription local_router, RouterDescription remote_router, Socket connection, ObjectInputStream in) throws IOException {
		this.local_router = local_router;
		this.remote_router = remote_router;
		this.out = new ObjectOutputStream(connection.getOutputStream());
		this.in = this.in;
	}*/

	public void send(Packet packet) throws IOException{
		out.writeObject(packet);
	}


	public Packet read(){
		Packet packetFromClient=null;
		try {
			packetFromClient = (Packet) in.readObject();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return packetFromClient;
	}
}