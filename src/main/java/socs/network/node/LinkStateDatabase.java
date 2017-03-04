package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;

import java.util.*;

public class LinkStateDatabase {

  //routerSimulatedIp => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription localRouterDescription = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    localRouterDescription = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.routerSimulatedIP, l);
  }

  /**
   *
   * @return {LinkedList<LinkDescription>} the neighbors of the local routers
   */
  public LinkedList<LinkDescription> getNeighbors(){
    return _store.get(localRouterDescription.simulatedIPAddress).links;
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

  
  public void newLSA(Link link){
	  LSA newLsa = new LSA(localRouterDescription.simulatedIPAddress, this.getNextLSASeqNum());
	  LSA oldLsa = _store.get(localRouterDescription.simulatedIPAddress);
	  newLsa.links.addAll(oldLsa.links);
	  newLsa.links.add(link.linkDescription);
	  _store.put(localRouterDescription.simulatedIPAddress, newLsa);
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



  public HashMap<String, HashMap<String, Integer>> convertDataBaseToGraph(){
    HashMap<String, HashMap<String, Integer>> graph = new HashMap<String, HashMap<String, Integer>>();

    for (LSA lsa : _store.values()){
      for (LinkDescription link : lsa.links){

        if (!graph.containsKey(lsa.routerSimulatedIP)){
          graph.put(lsa.routerSimulatedIP, new HashMap<String, Integer>());
        }

        graph.get(lsa.routerSimulatedIP).put(link.remoteRouter, link.weight);
      }

    }
    return graph;
  }
  /**

  public int[][] Dijkstra(HashMap<String, LSA> Graph, String source){
    Set<String> Q = new HashSet<String>();  //create vertex set Q

    //distance matrix, key: simulatedIpAddr, value:{int} distance
    HashMap<String, Integer> dist = new HashMap<String, Integer>();

    dist.put(localRouterDescription.simulatedIPAddress, 0); // Distance from source to source

    //distance from source to all its neighbors
    for (LinkDescription vertex : Graph.get(localRouterDescription.simulatedIPAddress).links){
      //dist.put(vertex.)
    }

  };**//**
          3      create vertex set Q
 4
         5      for each vertex v in Graph:             // Initialization
          6          dist[v] ← INFINITY                  // Unknown distance from source to v
 7          prev[v] ← UNDEFINED                 // Previous node in optimal path from source
 8          add v to Q                          // All nodes initially in Q (unvisited nodes)
 9
         10      dist[source] ← 0                        // Distance from source to source
          11
          12      while Q is not empty:
          13          u ← vertex in Q with min dist[u]    // Node with the least distance will be selected first
          14          remove u from Q
15
        16          for each neighbor v of u:           // where v is still in Q.
          17              alt ← dist[u] + length(u, v)
18              if alt < dist[v]:               // A shorter path to v has been found
          19                  dist[v] ← alt
20                  prev[v] ← u
21
        22      return dist[], prev[]
**/
}
