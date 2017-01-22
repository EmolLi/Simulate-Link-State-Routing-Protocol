package socs.network;

import socs.network.message.SOSPFPacket;
import socs.network.node.Router;
import socs.network.util.Client;
import socs.network.util.Configuration;

import socs.network.util.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Main {

  public static void main(String[] args) {
      //You cannot open a port below 1024, or you will have permission issue.
      //Check the port number first!
/**
      Server s = new Server((short)1450);
      s.startServer();


      //Client
      SOSPFPacket sos = new SOSPFPacket();
      sos.dstIP="fasdf";
      Client c = new Client();
      c.sendMsgToServer(1450,sos);

**/


    if (args.length != 2) {
      System.out.println("usage: program port_num conf_path");
      System.exit(1);
    }

    //user specifies the process port
    int port = Integer.parseInt(args[0]);

    Router r = new Router(new Configuration(args[1]), port);
    r.terminal();
  }
}
