package com.test.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Scanner;

public class ProxyServer implements Runnable {

	private ServerSocket serverSocket;

	private volatile boolean listening = true;

	// HashMap to hold the cached items
	static HashMap<String, File> cache;

	// main method for proxy server
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ProxyServer proxyServer = new ProxyServer(8085);
		proxyServer.listen();

	}

	public void run() {

		Scanner scanner = new Scanner(System.in);

		String cmd;
		while (listening) {
			cmd = scanner.nextLine();

			if (cmd.equals("close")) {
				listening = false;
				closeServer();
			}

		}
		scanner.close();
	}

	public void listen() {

		while (listening) {
			try {
				Socket socket = serverSocket.accept();
				Thread thread = new Thread(new RequestHandler(socket));
				thread.start();
			} catch (SocketException e) {
				System.out.println("Sever Socket Exception" + e.getStackTrace());
			} catch (IOException e) {
				e.printStackTrace();
			}
			// finally {
			// closeServer();
			// }
		}
	}

	public ProxyServer(int port) {

		cache = new HashMap<>();
		new Thread(this).start();

		try {
			// Create the Server Socket for the Proxy Server
			serverSocket = new ServerSocket(port);

			System.out.println("Proxy Server listening on port " + serverSocket.getLocalPort() + "..");
			listening = true;
		}

		catch (SocketException se) {
			System.out.println("Socket Exception" + se.getStackTrace());
			se.printStackTrace();
		} catch (SocketTimeoutException ste) {
			System.out.println("Timeout Exception" + ste.getStackTrace());
		} catch (IOException io) {
			System.out.println("IO exception " + io.getStackTrace());
		}
	}

	public static File getCache(String url) {
		return cache.get(url);
	}

	public static void setCache(String url, File file) {
		cache.put(url, file);
	}

	private void closeServer() {
		listening = false;

		// Close Server Socket
		try {
			System.out.println("Terminating Connection");
			serverSocket.close();
		} catch (Exception e) {
			System.out.println("Exception closing proxy's server socket");
			e.printStackTrace();
		}

	}

}
