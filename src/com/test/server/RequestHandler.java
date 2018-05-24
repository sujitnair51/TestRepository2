package com.test.server;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import javax.imageio.ImageIO;

public class RequestHandler implements Runnable {

//  Client socket passed by proxy
	Socket clientSocket;

//	read data from client
	BufferedReader reader;

//  write data to client
	BufferedWriter writer;

//  initialize the client socket, reader and writer	
	public RequestHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
		try {
			this.clientSocket.setSoTimeout(2000);
			reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();

		}
	}


	@Override
	public void run() {

		// request string from client
		String requestString;
		try {
			requestString = reader.readLine();
			System.out.println(requestString);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error reading request from client");
			return;
		}

		// Parse out Request String URL

		System.out.println("REQUEST STRING: " + requestString);
		
		// remove request type and space
		String urlString = requestString.substring(requestString.indexOf(' ') + 1);

		// Remove everything past next space
		urlString = urlString.substring(0, urlString.indexOf(' '));

		// Prepend http:// if necessary to create correct URL
		if (!urlString.substring(0, 4).equals("http")) {
			String temp = "http://";
			urlString = temp + urlString;
		}

		// Check if we have a cached copy
		File file;
		if ((file = ProxyServer.getCache(urlString)) != null) {
			System.out.println("Retrieving from Cache : " + urlString + "\n");
			getCachedFile(file);
		} else {
			System.out.println("HTTP GET for : " + urlString + "\n");
			addToCache(urlString);
		}
	}
	
	private void getCachedFile(File cachedFile) {
		// Read from File containing cached web page
		try {
			// If file is an image write data to client using buffered image.
			String fileExtension = cachedFile.getName().substring(cachedFile.getName().lastIndexOf('.'));

			// Response that will be sent to the server
			String response;
			if ((fileExtension.contains(".png")) || fileExtension.contains(".jpg") || fileExtension.contains(".jpeg")
					|| fileExtension.contains(".gif")) {
				// Read in image from storage
				BufferedImage image = ImageIO.read(cachedFile);

				if (image == null) {
					System.out.println("Image " + cachedFile.getName() + " was null");
					response = "HTTP/1.0 404 NOT FOUND \n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
					writer.write(response);
					writer.flush();
				} else {
					response = "HTTP/1.0 200 OK\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
					writer.write(response);
					writer.flush();
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
				}
			}

			// Standard text based file requested
			else {
				BufferedReader cachedFileBufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(cachedFile)));

				response = "HTTP/1.0 200 OK\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
				writer.write(response);
				writer.flush();

				String line;
				while ((line = cachedFileBufferedReader.readLine()) != null) {
					writer.write(line);
				}
				writer.flush();

				// Close resources
				if (cachedFileBufferedReader != null) {
					cachedFileBufferedReader.close();
				}
			}

			// Close Down Resources
			if (writer != null) {
				writer.close();
			}

		} catch (IOException e) {
			System.out.println("Error Sending Cached file to client");
			e.printStackTrace();
		}
	}

	
	private void addToCache(String urlString) {

		try {

			// Compute a logical file name as per schema
			// This allows the files on stored on disk to resemble that of the URL it was
			// taken from
			int fileExtensionIndex = urlString.lastIndexOf(".");
			String fileExtension;

			// Get the type of file
			fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

			// Get the initial file name
			String fileName = urlString.substring(0, fileExtensionIndex);

			// Trim off http://www. as no need for it in file name
			fileName = fileName.substring(fileName.indexOf('.') + 1);

			// Remove any illegal characters from file name
			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.', '_');

			// Trailing / result in index.html of that directory being fetched
			if (fileExtension.contains("/")) {
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.', '_');
				fileExtension += ".html";
			}

			fileName = fileName + fileExtension;

			// Attempt to create File to cache to
			boolean caching = true;
			File fileToCache = null;
			BufferedWriter fileToCacheBW = null;

			try {
				// Create File to cache
				fileToCache = new File("cached/" + fileName);

				if (!fileToCache.exists()) {
					fileToCache.createNewFile();
				}

				// Create Buffered output stream to write to cached copy of file
				fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
			} catch (IOException e) {
				System.out.println("Couldn't cache: " + fileName);
				caching = false;
				e.printStackTrace();
			} catch (NullPointerException e) {
				System.out.println("NPE opening file");
			}

			// Check if file is an image
			if ((fileExtension.contains(".png")) || fileExtension.contains(".jpg") || fileExtension.contains(".jpeg")
					|| fileExtension.contains(".gif")) {
				// Create the URL
				URL remoteURL = new URL(urlString);
				BufferedImage image = ImageIO.read(remoteURL);

				if (image != null) {
					// Cache the image to disk
					ImageIO.write(image, fileExtension.substring(1), fileToCache);

					// Send response code to client
					String line = "HTTP/1.0 200 OK\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
					writer.write(line);
					writer.flush();

					// Send them the image data
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

					// No image received from remote server
				} else {
					String error = "HTTP/1.0 404 NOT FOUND\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
					writer.write(error);
					writer.flush();
					return;
				}
			}

			// File is a text file
			else {

				// Create the URL
				URL remoteURL = new URL(urlString);
				// Create a connection to remote server
				HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);

				// Create Buffered Reader from remote Server
				BufferedReader proxyToServerBR = new BufferedReader(
						new InputStreamReader(proxyToServerCon.getInputStream()));

				// Send success code to client
				String line = "HTTP/1.0 200 OK\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
				writer.write(line);

				// Read from input stream between proxy and remote server
				while ((line = proxyToServerBR.readLine()) != null) {
					// Send on data to client
					writer.write(line);

					// Write to our cached copy of the file
					if (caching) {
						fileToCacheBW.write(line);
					}
				}

				// Ensure all data is sent by this point
				writer.flush();

				// Close Down Resources
				if (proxyToServerBR != null) {
					proxyToServerBR.close();
				}
			}

			if (caching) {
				// Ensure data written and add to our cached hash maps
				fileToCacheBW.flush();
				ProxyServer.setCache(urlString, fileToCache);
			}

			// Close down resources
			if (fileToCacheBW != null) {
				fileToCacheBW.close();
			}

			if (writer != null) {
				writer.close();
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

}