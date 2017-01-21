package ProxyProject;

import java.io.*;
import java.net.*;

public class ProxyServer {
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = null;

		int port = 8970;

		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Started on: " + port);
		} catch (IOException e) {
			System.err.println("Could not listen on port: " + port);
			System.exit(-1);
		}

		try {
			while (true) {
				//serverSocket.accept();
				new HttpRequest(serverSocket.accept()).parsing();
			}
		} catch (Exception ex) {
			System.err.println(ex.getMessage());

		} finally {
			serverSocket.close();
		}
	}
}
