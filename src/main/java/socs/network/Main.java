package socs.network;

import socs.network.node.Router;
import socs.network.util.Configuration;

import socs.network.util.Server;


public class Main {

  public static void main(String[] args) {
      //You cannot open a port below 1024, or you will have permission issue.
      //Check the port number first!

      Server s = new Server((short)1450);
      s.startServer();


/**
    if (args.length != 2) {
      System.out.println("usage: program port_num conf_path");
      System.exit(1);
    }

    //user specifies the process port
    int port = Integer.parseInt(args[0]);

    Router r = new Router(new Configuration(args[1]), port);
    r.terminal();**/
  }
}
