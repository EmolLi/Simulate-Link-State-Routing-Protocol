package socs.network.node;

public class RouterDescription {
	//used to socket communication
	String processIPAddress="127.0.0.1";
	short processPortNumber;
	//used to identify the router in the simulated network space
	String simulatedIPAddress;
	//status of the router
	RouterStatus status;
	
	public RouterDescription(String simulatedIP, short processPort) {
		this.simulatedIPAddress = simulatedIP;
		this.processPortNumber = processPort;
	}
}
