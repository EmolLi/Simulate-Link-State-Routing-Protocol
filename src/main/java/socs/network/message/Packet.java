package socs.network.message;

import java.io.*;
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
	public int packetType; //0 - HELLO, 1 - LinkState Update 2 - Attach
	public String routerID;

	//used by HELLO message to identify the sender of the message
	//e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
	//simulated IP address
	public String neighborID; //neighbor's simulated IP address

	//used by LSAUPDATE
	public Vector<LSA> lsaArray = null;

	public Packet(String simulatedSrcIP, String simulatedDstIP, int packetType) {
		this.simulatedSrcIP = simulatedSrcIP;
		this.simulatedDstIP = simulatedDstIP;
		this.packetType = packetType;
	}

}
