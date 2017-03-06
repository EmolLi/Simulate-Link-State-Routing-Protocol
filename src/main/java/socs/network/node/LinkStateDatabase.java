package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;

import java.util.*;

public class LinkStateDatabase {

    //routerSimulatedIp => LSAInstance
    volatile HashMap<String, LSA> _store = new HashMap<String, LSA>();

    private RouterDescription localRouterDescription = null;
    public HashMap<String, Integer> dist;   //distance matrix, key: simulatedIpAddr, value:{int} distance
    public HashMap<String, String> prev;    // Previous node in optimal path from source

    public LinkStateDatabase(RouterDescription routerDescription) {
        localRouterDescription = routerDescription;
        LSA l = initLinkStateDatabase();
        _store.put(l.routerSimulatedIP, l);
    }

    /**
     * @return {LinkedList<LinkDescription>} the neighbors of the local routers
     */
    public LinkedList<LinkDescription> getNeighbors() {
        return _store.get(localRouterDescription.simulatedIPAddress).links;
    }

    public LSA getLSA(String routerSimulatedIP) {
        return _store.get(routerSimulatedIP);
    }



    public boolean updateLSA(LSA lsa){
        String simulatedIP = lsa.routerSimulatedIP;
        if (_store.containsKey(simulatedIP)) {
            int curSeqNum = _store.get(simulatedIP).lsaSeqNumber;
            int newSeqNum = lsa.lsaSeqNumber;

            if (curSeqNum >= newSeqNum) {
                return false;
            }
            _store.put(simulatedIP, lsa);
            System.out.println("UPDATE SUCCESSFULLY: " + lsa.toString());
            return true;
        } else {
            _store.put(simulatedIP, lsa);
            System.out.println("INCLUDED NEW ENTRY INTO DB: " + lsa.toString());
            return true;
        }
    }


    public void addNewLinkToDB(Link link) {
        LSA newLsa = new LSA(localRouterDescription.simulatedIPAddress, this.getNextLSASeqNum());
    
        LSA oldLsa = _store.get(localRouterDescription.simulatedIPAddress);
        System.out.println(oldLsa);
        System.out.println(link);
        newLsa.links.addAll(oldLsa.links);
        newLsa.links.add(link.linkDescription);
        _store.put(localRouterDescription.simulatedIPAddress, newLsa);
    }

    public int getNextLSASeqNum() {
        int lastSeqNum = getLSA(localRouterDescription.simulatedIPAddress).lsaSeqNumber;
        return lastSeqNum + 1;
    }

    /**
     * output the shortest path from this router to the destination with the given IP address
     */
    String getShortestPath(String destinationIP) {
        if (!this.hasEntryFor(destinationIP)){
            System.out.println("Unknown destination IP.");
            return null;
        }
        HashMap<String, HashMap<String, Integer>> graph = convertDataBaseToGraph();
        Dijkstra(graph);
        return formatPath(destinationIP);
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
        for (LSA lsa : _store.values()) {
            sb.append(lsa.routerSimulatedIP).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
            for (LinkDescription ld : lsa.links) {
                sb.append(ld.remoteIP).append(",").append(ld.portNum).append(",").
                        append(ld.weight).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    //----------------------------Helper functions for computing the shortest path------------------------------------

    /**
     * this function convert database to a graph, that's easier to run Dijkstra
     *
     * @return
     */

    private HashMap<String, HashMap<String, Integer>> convertDataBaseToGraph() {
        HashMap<String, HashMap<String, Integer>> graph = new HashMap<String, HashMap<String, Integer>>();

        for (LSA lsa : _store.values()) {
            for (LinkDescription link : lsa.links) {

                if (!graph.containsKey(lsa.routerSimulatedIP)) {
                    graph.put(lsa.routerSimulatedIP, new HashMap<String, Integer>());
                }

                graph.get(lsa.routerSimulatedIP).put(link.remoteIP, link.weight);
            }
        }
        return graph;
    }


    /**
     * this function modifies dist and prev table. compute the shortest path
     *
     * @param Graph the graph version of the database, so it's easier to run dijkstra
     */
    private void Dijkstra(HashMap<String, HashMap<String, Integer>> Graph) {
        String src = localRouterDescription.simulatedIPAddress;

        Set<String> Q = new HashSet<String>();  //create vertex set Q

        //distance matrix, key: simulatedIpAddr, value:{int} distance
        dist = new HashMap<String, Integer>();
        // Previous node in optimal path from source
        prev = new HashMap<String, String>();

        for (String vertex : Graph.keySet()) {    // Initialization
            dist.put(vertex, Integer.MAX_VALUE);    // Unknown distance from source to v
            prev.put(vertex, null);
            Q.add(vertex);  // All nodes initially in Q (unvisited nodes)
        }
        dist.put(localRouterDescription.simulatedIPAddress, 0); // Distance from source to source

        while (!Q.isEmpty()) {
            String u = findClosestUnvisitedVertex(Q, dist);
            Q.remove(u);

            //for each neighbor v of u
            for (String v : Graph.get(u).keySet()) {
                int alt = dist.get(u) + Graph.get(u).get(v);
                if (alt < dist.get(v)) {    // A shorter path to v has been found
                    dist.put(v, alt);
                    prev.put(v, u);
                }
            }
        }
    }


    public boolean hasEntryFor(String routerSimulatedIP) {
        return _store.containsKey(routerSimulatedIP);
    }

    public ArrayList<LSA> getLSAs() {
        ArrayList<LSA> lsas = new ArrayList<LSA>();
        synchronized (_store) {
            for (String ip : _store.keySet()) {
                lsas.add(_store.get(ip));
            }
        }
        return lsas;
    }


    private String findClosestUnvisitedVertex(Set<String> Q, HashMap<String, Integer> dist) {
        int minDist = Integer.MAX_VALUE;
        String closestVertex = null;

        for (String vertex : Q) {
            if (dist.get(vertex) < minDist) {
                closestVertex = vertex;
                minDist = dist.get(vertex);
            }
        }

        return closestVertex;
    }

    /**
     * this function should be executed after Dijkstra.
     * it reads the result computed by Dijkstra and converted a nicer formatted string
     *
     * @param destinationIP
     * @return path like 192.168.1.2 ->(4) 192.168.1.5 ->(3) 192.168.1.3 ->(2) 192.168.1.6
     */
    private String formatPath(String destinationIP) {
        String router = destinationIP;  //destination
        String path = "";

        while (!router.equals(localRouterDescription.simulatedIPAddress)) {
            path = router + path;
            try {
                String weight = "(" + _store.get(router).getLinkDescription(prev.get(router)).weight + ")";
                path = prev.get(router) + " ->" + weight + path;
            } catch (Exception e) {
                e.printStackTrace();
            }
            //move router one back step along the path to src
            router = prev.get(router);
        }

        return path;
    }

    //------------------------------------------------------------------------------------------------------


}
