package ProxyProject;

import java.io.*;
import java.util.*;
import java.net.*;

public class HttpRequest {

	private Socket socket = null;

	public HttpRequest(Socket socket) {

		this.socket = socket;
	}

	public void parsing() throws IOException {
		BufferedReader ip = null;
		DataOutputStream to = null;
		String inputLine;

		try {

			ip = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println("Something");

			while ((inputLine = ip.readLine()) != null) {
				// try {
				// StringTokenizer tokens = new StringTokenizer(inputLine);
				// tokens.nextToken();
				// } catch (Exception e) {
				// break;
				// }
				
				if (inputLine == null || inputLine.equals("")) {
					throw new IOException("Request line is null");
				}

				String[] tokens = inputLine.split(" ");
				if (tokens.length < 3) {
					throw new Exception("Incomplete tokens");
				} else {
					String method = tokens[0];
					String uri = tokens[1];
					String version = tokens[2];

					if (!method.equals("GET")) {
						throw new Exception("not get");
					}

					URL url = new URL(uri);
					String host = url.getHost();
					if (host == null || host.equals("")) {
						throw new Exception("host wrong");
					}

					int port = url.getPort();
					String protocol = url.getProtocol();
					String path = url.getPath();
					if (path == null || path.equals("")) {
						path = "/";
					}

					if (port == -1) {
						if (protocol.equals("http")) {
							port = 80;// default
						} else {
							throw new Exception("wrong portcio");
						}
					}

					String query = url.getQuery();
					if (query != null && !query.equals("")) {
						path += "?" + query;

					}

					if (!version.equals("HTTP/1.0")) {
						version = "HTTP/1.0";
					}
					Socket newSocket = new Socket(host, port);
					to = new DataOutputStream(newSocket.getOutputStream());
					String toWrite = method + " " + path + " " + version + "\r\n" + "Host: " + host + "\r\n"
							+ "Connection: close\r\n\r\n";
					to.writeBytes(toWrite);

					to.flush();

					BufferedReader rd = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
					String line;
					StringBuilder response = new StringBuilder();
					response.append("\r\n");
					while ((line = rd.readLine()) != null) {
						response.append(line + "\r\n");
				
					}
					DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
					toClient.writeBytes(response.toString());
				
					toClient.flush();
					toClient.close();
					to.close();
					try {
						if (newSocket != null)
							newSocket.close();
					} catch (IOException e) {

					}
				}

			}

		} catch (Exception e) {

		} finally {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {

			}

		}
	}
}

// URL url = new URL("http://www.runoob.com/index.html?language=cn#j2se");
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
