package socs.network.message;

import java.io.Serializable;

public class LinkDescription implements Serializable {
  public String remoteRouter;
  public int portNum;
  public int weight;

  public LinkDescription(String remoteRouter, int portNum, int weight){
    this.remoteRouter = remoteRouter;
    this.portNum = portNum;
    this.weight = weight;
  }

  public String toString() {
    return remoteRouter + ","  + portNum + "," + weight;
  }
}
