package socs.network.node;

import socs.network.util.Server;

public class RouterDescription {
    //used to socket communication
    String processIPAddress;
    short processPortNumber;

    //used to identify the router in the simulated network space
    String simulatedIPAddress;
    //status of the router
    RouterStatus status;

    public RouterDescription(String processIPAddress, short processPortNumber, String simulatedIPAddress){
        this.processIPAddress = processIPAddress;
        this.processPortNumber = processPortNumber;
        this.simulatedIPAddress = simulatedIPAddress;
        //initially routerStatus is null
    }

    /**
     * check if two rd are equal
     * @param rd
     * @return
     * @throws Exception
     */
    public boolean isEqual(RouterDescription rd) throws Exception{
        if (this.simulatedIPAddress.equals(rd.simulatedIPAddress)){
            if (this.processIPAddress.equals(rd.processIPAddress) && this.processPortNumber == rd.processPortNumber) return true;
            throw new Exception("Error in router initializing. Found routers with same same simulated Ip address.");
        }
        return false;
    }

    public void printFull(){
        System.out.println("Router (simulated IP address: "+simulatedIPAddress+" , process Port Num: "+processPortNumber + " , process IP Address: "+ processIPAddress);
    }

    public void print(){
        System.out.println(simulatedIPAddress);
    }

}
