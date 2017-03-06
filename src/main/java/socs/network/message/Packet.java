package socs.network.message;

import java.io.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class Packet implements Serializable {
	//for inter-process communication
	public String srcProcessIP;
	public short srcProcessPort;

	//simulated IP address
	public String simulatedSrcIP;
	public String simulatedDstIP;

	//common header
	public int packetType; //0 - HELLO, 1 - LinkState Update
	public String routerID;
	//public int TTL = 20;

	//used by HELLO message to identify the sender of the message
	//e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
	//simulated IP address
	public String neighborID; //neighbor's simulated IP address

	//used by LSAUPDATE
	public ArrayList<LSA> lsaArray = null;

	//used for attach (link establishment)
	public int weight;

	public Packet(String simulatedSrcIP, String simulatedDstIP, int packetType) {
		this.simulatedSrcIP = simulatedSrcIP;
		this.simulatedDstIP = simulatedDstIP;
		this.packetType = packetType;
	}

	//create a LinkState Update package
	public static Packet LSAUPDATE(String simulatedSrcIP, String simulatedDstIP, ArrayList<LSA> lsaArray){
		Packet packet = new Packet(simulatedSrcIP, simulatedDstIP, 1);
		packet.lsaArray = lsaArray;
		return packet;
	}

	//create a AttachLinkRequest packet used in attach()
	public static Packet AttachLinkRequest(String simulatedSrcIP, String simulatedDstIP, int weight){
		Packet packet = new Packet(simulatedSrcIP, simulatedDstIP, 2);
		packet.weight = weight;
		return packet;
	}
	
	public String print(){
		StringBuilder str = new StringBuilder();
		str.append(" srcProcessIp: "+this.srcProcessIP);
		str.append(" srcProcessPort: "+this.srcProcessPort);
		str.append(" simulatedSrcIP: "+this.simulatedSrcIP);
		str.append(" simylatedDstIP: "+this.simulatedDstIP);
		str.append(" packetType: "+this.packetType); //0 - HELLO, 1 - LinkState Update
		str.append(" routerID: "+this.routerID);
		str.append(" neighborID: "+this.neighborID); //neighbor's simulated IP address
		return str.toString();	
	}
}
