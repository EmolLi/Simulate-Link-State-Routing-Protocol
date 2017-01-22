package socs.network.util;

import socs.network.message.SOSPFPacket;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server listens for client requests, and creates a clientTask for each request.
 * Client Task class is responsible for processing client request
 */



//Maybe use private class?
public class ClientTask implements Runnable{
    private final Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public ClientTask(Socket clientSocket){

        this.clientSocket = clientSocket;
    }

    public void run(){
        System.out.println("Got a client!");


        try {
            inputStream = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                //if there is no more packet, readObject will throw IOException, which will be caught
                SOSPFPacket sos = (SOSPFPacket) inputStream.readObject();
                System.out.println("Got a packet!");

            }
        }
        catch (EOFException e){
            System.out.println("No more packet!");
        }
        catch(Exception e){
            //
            e.printStackTrace();
        }

        try {
            clientSocket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }



}
