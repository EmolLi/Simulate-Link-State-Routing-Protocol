package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;

import java.util.HashMap;

public class LinkStateDatabase {

  //routerSimulatedIp => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription localRouterDescription = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    localRouterDescription = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.routerSimulatedIP, l);
  }

  public LSA getLSA(String routerSimulatedIP){
    return _store.get(routerSimulatedIP);
  }


  public void processLSAUPDATEPacket(Packet LSAUPDATE){
    //LSAUPDATE.
  }
  public void updateLSA(LSA lsa) throws Exception{
    String simulatedIP = lsa.routerSimulatedIP;
    int curSeqNum = _store.get(simulatedIP).lsaSeqNumber;
    int newSeqNum = lsa.lsaSeqNumber;

    if (curSeqNum >= newSeqNum){
      //no need to update
      throw new Exception("No need to update LSA!");
    }

    _store.put(simulatedIP, lsa);
    System.out.println("UPDATE SUCCESSFULLY: " + lsa.toString());
  }

  public int getNextLSASeqNum(){
    int lastSeqNum = getLSA(localRouterDescription.simulatedIPAddress).lsaSeqNumber;
    return lastSeqNum + 1;
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {
    //TODO: fill the implementation here
    return null;
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA(localRouterDescription.simulatedIPAddress, Integer.MIN_VALUE);

    LinkDescription ld = new LinkDescription(localRouterDescription.simulatedIPAddress, -1, 0);
    lsa.links.add(ld);
    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.routerSimulatedIP).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.remoteRouter).append(",").append(ld.portNum).append(",").
                append(ld.weight).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
