package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Client;
import socs.network.util.Configuration;
import socs.network.util.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class Router {

    protected LinkStateDatabase lsd;
    // this is the mapping between process IP, port and simulated ip
    protected HashMap<String, RouterDescription> rdMap = new HashMap<String, RouterDescription>();
    // this is the mapping between neighbor and port number (not process port), port for link. SimulatedIP -> portNum
    HashMap<String, Integer> neighborMap = new HashMap<String, Integer>();

    RouterDescription rd;

    //assuming that all routers are with 4 ports
    Link[] ports = new Link[4];

    int nextLsaSeqNum;
    Server server;
    Client client = new Client();



    public Router(Configuration config, int processPort) {
        String processIPAddress = "127.0.0.1";
        String simulatedIPAddress = config.getString("socs.network.router.ip");

        this.rd = new RouterDescription(processIPAddress, (short) processPort, simulatedIPAddress);

        rdMap.put(processIPAddress, rd);
        lsd = new LinkStateDatabase(rd);
        nextLsaSeqNum = 0;
    }


    /**
     * @param simulatedIP {String}
     * @return {RouterDescription} if any
     */
    private RouterDescription getRouterDescripBySimulatedIP(String simulatedIP) {
        return rdMap.get(simulatedIP);
    }

    /**
     * @return int The index of the unused port if any, -1 if all the ports are used, -2 if error found in routers initialization
     */
    private int findUnusedPort(RouterDescription remoteRouterDescription) {
        int unusedPort = -1;
        for (int i = 0; i < ports.length; i++) {
            Link link = ports[i];

            if (link == null){
                if (unusedPort == -1) unusedPort = i;
            }
            else {

                try {
                    if (link.router1.isEqual(remoteRouterDescription) || link.router2.isEqual(remoteRouterDescription))
                        System.err.println("This router is already connected with the remote router.");
                } catch (Exception e) {
                    e.printStackTrace();
                    unusedPort = -2;
                }
            }
        }
        return unusedPort;
    }

    /**
     * output the shortest path to the given destination ip
     * <p/>
     * format: source ip address  -> ip address -> ... -> destination ip
     *
     * @param destinationIP the ip adderss of the destination simulated router
     */
    private void processDetect(String destinationIP) {

    }

    /**
     * disconnect with the router identified by the given destination ip address
     * Notice: this command should trigger the synchronization of database
     *
     * @param portNumber the port number which the link attaches at
     */
    private void processDisconnect(short portNumber) {

    }

    /**
     * attach the link to the remote router, which is identified by the given simulated ip;
     * to establish the connection via socket, you need to indentify the process IP and process Port (Q: the process IP & port of this router or the remote router??) ;
     * additionally, weight is the cost to transmitting data through the link
     * <p/>
     * NOTE: this command should not trigger link database synchronization
     */
    //Now I just assume processIp & port & simulatedIp all belong to the remote router
    private void processAttach(String processIP, short processPort,
                               String simulatedIP, short weight) {
        //check if the processIp, processPort, simulatedIp is correct

        //if all the four ports of the remote router are used

        //rd for remote router
        RouterDescription rd2 = new RouterDescription(processIP, processPort, simulatedIP);

        //update rdMap if rd2 the remote router is not in map -> this actually doesnt matter
        rdMap.put(processIP, rd2);

        int port = findUnusedPort(rd2);

        //if this router is able to connect to another router, positive port num will be returned
        if (port >= 0) {
            Link link = new Link(this.rd, rd2, weight);
            ports[port] = link;
            //update neighbor map
            neighborMap.put(simulatedIP, port);
        }
    }

    /**
     * broadcast Hello to neighbors
     */
    private void processStart() {
        initServer();

        ArrayList<RouterDescription> neighbors = getNeighborsRD();

        for (RouterDescription neighbor : neighbors){
            SOSPFPacket packet = new SOSPFPacket();
            client.sendMsgToServer(neighbor.processPortNumber, packet);
        }


    }

    /**
     * attach the link to the remote router, which is identified by the given simulated ip;
     * to establish the connection via socket, you need to indentify the process IP and process Port;
     * additionally, weight is the cost to transmitting data through the link
     * <p/>
     * This command does trigger the link database synchronization
     */
    private void processConnect(String processIP, short processPort,
                                String simulatedIP, short weight) {

    }


    /**
     * output the neighbors of the routers
     */
    private void processNeighbors() {
        for (String simulatedIP :  neighborMap.keySet()){
            System.out.println(simulatedIP);
        }
    }

    private ArrayList<RouterDescription> getNeighborsRD() {
        ArrayList<RouterDescription> neighbors = new ArrayList<RouterDescription>();
        for (Integer i : neighborMap.values()){
            neighbors.add(ports[i].router2);
        }
        return neighbors;
    }


    private RouterDescription getNeighborRDBySIP(String simulatedIP){
        int port = neighborMap.get(simulatedIP);
        return ports[port].router2;
    }

    /**
     * send packet to remote router
     * @param simulatedIp
     * @param packet
     */
    private void send(String simulatedIp, SOSPFPacket packet){

    }

    /**
     * start to listen for incoming packages
     */
    private void initServer(){
        server = new Server(rd.processPortNumber);
        server.startServer();
    }

    /**
     * disconnect with all neighbors and quit the program
     */
    private void processQuit() {

    }

    public void terminal() {
        try {
            InputStreamReader isReader = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(isReader);
            System.out.print(">> ");
            String command = br.readLine();
            while (true) {
                if (command.startsWith("detect ")) {
                    String[] cmdLine = command.split(" ");
                    processDetect(cmdLine[1]);
                } else if (command.startsWith("disconnect ")) {
                    String[] cmdLine = command.split(" ");
                    processDisconnect(Short.parseShort(cmdLine[1]));
                } else if (command.startsWith("quit")) {
                    processQuit();
                } else if (command.startsWith("attach ")) {
                    String[] cmdLine = command.split(" ");
                    processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                            cmdLine[3], Short.parseShort(cmdLine[4]));
                } else if (command.equals("start")) {
                    processStart();
                } else if (command.equals("connect ")) {
                    String[] cmdLine = command.split(" ");
                    processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                            cmdLine[3], Short.parseShort(cmdLine[4]));
                } else if (command.equals("neighbors")) {
                    //output neighbors
                    processNeighbors();
                } else {
                    //invalid command
                    break;
                }
                System.out.print(">> ");
                command = br.readLine();
            }
            isReader.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
