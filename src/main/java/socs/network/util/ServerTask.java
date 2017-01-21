package socs.network.util;

import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
/**
 * ServerTask is responsible for listening incoming client requests. It then delegates the client request to
 * ClientTask (threads in threadspool) to process the request.
 **/
public class ServerTask implements Runnable {
    private int port;
    protected ExecutorService threadsPool;

    public ServerTask(short port, ExecutorService threadsPool){
        this.port = (int) port;
        this.threadsPool = threadsPool;
    }

    public void run(){
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server is listening at port "+port);

            while (true){
                Socket clientSocket = serverSocket.accept();
                threadsPool.submit(new ClientTask(clientSocket));
            }
        }
        catch (IOException e){
            System.err.println("Unable to process client request");
            e.printStackTrace();
        }
    }
}
