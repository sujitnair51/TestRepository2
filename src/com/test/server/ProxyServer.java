package com.test.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ProxyServer implements Runnable {
	
	static ServerSocket serverSocket;
	
	static volatile boolean listening = true;
	
	// main method for proxy server
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		        
        ProxyServer proxyServer = new ProxyServer(10001);
        proxyServer.listen();
        
    }
	
	public void run() {
		
		// do something
	}
	
	
	public void listen(){

		while(listening){
			try {
				Socket socket = serverSocket.accept();
				Thread thread = new Thread(new RequestHandler(socket));
				thread.start();	
			} catch (SocketException e) {
				System.out.println("Sever Socket Exception"+ e.getStackTrace());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public ProxyServer(int port) {

			
		new Thread(this).start();	

		
		try {
			// Create the Server Socket for the Proxy Server
			serverSocket = new ServerSocket(port);

			System.out.println("Proxy Server listening on port " + serverSocket.getLocalPort() + "..");
			listening = true;
		} 

		catch (SocketException se) {
			System.out.println("Socket Exception"+ se.getStackTrace());
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout Exception"+ ste.getStackTrace());
		} 
		catch (IOException io) {
			System.out.println("IO exception "+ io.getStackTrace());
		}
	}

}
