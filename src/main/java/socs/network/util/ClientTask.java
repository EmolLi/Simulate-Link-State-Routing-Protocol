package socs.network.util;

import java.io.IOException;
import java.net.Socket;

/**
 * Server listens for client requests, and creates a clientTask for each request.
 * Client Task class is responsible for processing client request
 */



//Maybe use private class?
public class ClientTask implements Runnable{
    private final Socket clientSocket;

    public ClientTask(Socket clientSocket){
        this.clientSocket = clientSocket;
    }
    public void run(){
        System.out.println("Got a client!");

        /**
         * Process client request here
         */

        try {
            clientSocket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }



}
