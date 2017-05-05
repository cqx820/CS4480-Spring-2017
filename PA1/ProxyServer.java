//package ProxyProject;

import java.io.*;
import java.net.*;

public class ProxyServer {
	public static void main(String[] args) throws IOException {

		ServerSocket serverSocket = null;

		int port = 6666;

		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Started on: " + port);
		} catch (IOException e) {
			System.err.println("Could not listen on port: " + port);
			System.exit(-1);
		}

		try {
			while (true) {
				// serverSocket.accept();
				new Thread(new HttpRequest(serverSocket.accept())).start();
			}
		} catch (Exception ex) {
			System.err.println(ex.getMessage());

		} finally {
			try {
				if (serverSocket != null)
					serverSocket.close();
			} catch (IOException e) {
				System.err.println("Unable to close socket");
			}
		}

		// URL url = new
		// URL("http://www.runoob.com/index.html?language=cn#j2se");
		// System.out.println("URL \u4e3a\uff1a" + url.toString());
		// System.out.println("\u534f\u8bae\u4e3a\uff1a" + url.getProtocol());
		// System.out.println("\u9a8c\u8bc1\u4fe1\u606f\uff1a" + url.getAuthority());
		// System.out.println("\u6587\u4ef6\u540d\u53ca\u8bf7\u6c42\u53c2\u6570\uff1a" + url.getFile());
		// System.out.println("\u4e3b\u673a\u540d\uff1a" + url.getHost());
		// System.out.println("\u8def\u5f84\uff1a" + url.getPath());
		// System.out.println("\u7aef\u53e3\uff1a" + url.getPort());
		// System.out.println("\u9ed8\u8ba4\u7aef\u53e3\uff1a" + url.getDefaultPort());
		// System.out.println("\u8bf7\u6c42\u53c2\u6570\uff1a" + url.getQuery());
		// System.out.println("\u5b9a\u4f4d\u4f4d\u7f6e\uff1a" + url.getRef());
	}
}
