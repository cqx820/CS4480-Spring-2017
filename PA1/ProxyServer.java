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
				// serverSocket.accept();
				new HttpRequest(serverSocket.accept()).parsing();
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
		// System.out.println("URL 为：" + url.toString());
		// System.out.println("协议为：" + url.getProtocol());
		// System.out.println("验证信息：" + url.getAuthority());
		// System.out.println("文件名及请求参数：" + url.getFile());
		// System.out.println("主机名：" + url.getHost());
		// System.out.println("路径：" + url.getPath());
		// System.out.println("端口：" + url.getPort());
		// System.out.println("默认端口：" + url.getDefaultPort());
		// System.out.println("请求参数：" + url.getQuery());
		// System.out.println("定位位置：" + url.getRef());
	}
}
