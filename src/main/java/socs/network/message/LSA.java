package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

public class LSA implements Serializable {

  //IP address of the router originate this LSA
  public String routerSimulatedIP;
  public int lsaSeqNumber = Integer.MIN_VALUE;

  public LinkedList<LinkDescription> links = new LinkedList<LinkDescription>();

  public LSA(String routerSimulatedIP, int lsaSeqNumber){
    this.routerSimulatedIP = routerSimulatedIP;
    this.lsaSeqNumber = lsaSeqNumber;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("routerSimulatedIp: "+routerSimulatedIP).append(" lsaSeqNumber: "+lsaSeqNumber + "\n");
    sb.append("[");
    for (LinkDescription ld : links) {
      sb.append(ld);
    }
    sb.append("]");
    sb.append("\n");
    return sb.toString();
  }
}
