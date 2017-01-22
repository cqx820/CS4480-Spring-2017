package ProxyProject;

import java.io.*;
import java.net.*;
import javax.xml.ws.http.HTTPException;

public class HttpRequest {
	private final int BAD_REQUEST_CODE = 400;
	private final String BAD_REQUEST_MESSAGE = "Bad Request";
	private final int NOT_IMPLEMENTED_CODE = 501;
	private final String NOT_IMPLEMENTED_MESSAGE = "Not Implemented";
	private Socket socket = null;

	public HttpRequest(Socket socket) {
		this.socket = socket; // Client socket
	}

	public void parsing() throws IOException {
		BufferedReader ip = null;
		DataOutputStream to = null;
		String inputLine;
		try {
			ip = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println("Please enter your request, \"GET\" request only");
			String protocol, host, path, query;
			int port;
			while ((inputLine = ip.readLine()) != null) {
				// try {
				// StringTokenizer tokens = new StringTokenizer(inputLine);
				// tokens.nextToken();
				// } catch (Exception e) {
				// break;
				// }
				if (inputLine == null || inputLine.equals("")) {
					System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
					throw new HTTPException(BAD_REQUEST_CODE);
					// throw new IOException("Please enter a conrrect request");
				}
				String[] tokens = inputLine.split(" ");
				if (tokens.length < 3) {
					System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
					throw new HTTPException(BAD_REQUEST_CODE);
					// throw new Exception("Incomplete request");
				} else {
					String method = tokens[0];
					String uri = tokens[1];
					String version = tokens[2];

					if (!method.equals("GET")) {
						if (method.equals("HEAD") || method.equals("POST") || method.equals("DELETE")
								|| method.equals("PUT") || method.equals("OPTIONS") || method.equals("TRACE")
								|| method.equals("CONNECT")) {
							System.err.println("Error Code " + NOT_IMPLEMENTED_CODE + ": " + NOT_IMPLEMENTED_MESSAGE);
							throw new HTTPException(NOT_IMPLEMENTED_CODE);

						} else {
							System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
							throw new HTTPException(BAD_REQUEST_CODE);
							// throw new Exception("\"GET\" method only");
						}
					}
					URL url = new URL(uri);
					host = url.getHost();
					if (host == null || host.equals("")) {
						System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
						throw new HTTPException(BAD_REQUEST_CODE);
						// throw new Exception("Unable to get host info");
					}
					port = url.getPort();
					protocol = url.getProtocol();
					path = url.getPath();
					if (path == null || path.equals("")) {
						path = "/";
					}
					if (port == -1) {
						if (protocol.equals("http")) {
							port = 80;// default port
						} else {
							System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
							throw new HTTPException(BAD_REQUEST_CODE);
							// throw new Exception("Wrong protocol, please
							// check");
						}
					}
					query = url.getQuery();
					if (query != null && !query.equals("")) {
						path += "?" + query;

					}
					if (!version.equals("HTTP/1.0")) {
						version = "HTTP/1.0";
						System.out.println("HTTP version is changed");
					}
					Socket newSocket = new Socket(host, port);// New socket to
																// connect
																// server
					to = new DataOutputStream(newSocket.getOutputStream());
					String toWrite = method + " " + path + " " + version + "\r\n" + "Host: " + host + "\r\n"
							+ "Connection: close\r\n\r\n";
					to.writeBytes(toWrite);// Send request to server
					to.flush();
					BufferedReader rd = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
					String line;
					StringBuilder response = new StringBuilder();
					response.append("\r\n");
					while ((line = rd.readLine()) != null) {
						response.append(line + "\r\n"); // receive and build
														// response string from
														// server
					}
					DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
					toClient.writeBytes(response.toString()); // Write response
																// to client
					toClient.flush();
					toClient.close();
					to.close();
					try {
						if (newSocket != null)
							newSocket.close();
					} catch (IOException e) {
						System.err.println("Unable to close socket");
					}
				}
			}
		} catch (Exception e) {
			if (e.getMessage() != null)
				System.err.println(e.getMessage());
		} finally {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				System.err.println("Unable to close socket");
			}
		}
	}
}
