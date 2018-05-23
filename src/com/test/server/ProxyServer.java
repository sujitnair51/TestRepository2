package com.test.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ProxyServer implements Runnable {
	
	static ServerSocket serverSocket;
	
	static volatile boolean listening = true;
	
	// main method for proxy server
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		    

        int port = 10001;	//default
        
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Proxy Server listening on: " + port);
            
        } catch (IOException e) {
            System.out.println("IO Exception! "+e.getMessage());
            System.exit(-1);
        }
        try {
        	while (listening) {
                new RequestHandler(serverSocket.accept()).start();
            }
        	serverSocket.close();
        }catch(Exception e)
        {
        	System.out.println("Exception! "+e.getMessage());
        }
        finally {
        	
        }
        
    }
	
	
	
	public void run() {}
	// do something

}
