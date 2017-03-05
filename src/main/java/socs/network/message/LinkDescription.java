package socs.network.message;

import java.io.Serializable;

public class LinkDescription implements Serializable {
  public String remoteIP;
  public int portNum;
  public int weight;

  public LinkDescription(String remoteRouter, int portNum, int weight){
    this.remoteIP = remoteRouter;
    this.portNum = portNum;
    this.weight = weight;
  }

  public String toString() {
    return  "remoteRouter: "+remoteIP + " portNumber: "  + portNum + " weight: " + weight+" ";
  }
}
