package socs.network.util;

import java.net.ServerSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Every router is both a server and client
 * Server class implements the server utilities.
 *
 * The work is split to ServerTask and ClientTask.
 *
 * ServerTask is responsible for listening incoming client requests. It then delegates the client request to
 * ClientTask (threads in threadspool) to process the request.
 */
public class Server{
    protected short port;
    protected ServerSocket serverSocket = null;

    public Server(short port){
        this.port = port;
    }

    public void startServer(){
        //1 thread to listening for incoming request, four for 4 clients (4 ports)
        final ExecutorService threadPool = Executors.newFixedThreadPool(5);

        ServerTask serverTask = new ServerTask(port, threadPool);

        Thread serverThread = new Thread(serverTask);
        serverThread.start();

    }


}

