package ProxyProject;

import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.concurrent.locks.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.ws.http.HTTPException;

public class HttpRequest implements Runnable {
	// Constant of error code and error message
	private final int BAD_REQUEST_CODE = 400;
	private final String BAD_REQUEST_MESSAGE = "Bad Request";
	private final int NOT_IMPLEMENTED_CODE = 501;
	private final String NOT_IMPLEMENTED_MESSAGE = "Not Implemented";
	private Socket socket = null;
	private ReentrantLock lock;

	public HttpRequest(Socket socket) {
		this.socket = socket; // Client socket
		this.lock = new ReentrantLock();
	}

	public void run() {
		BufferedReader ip = null;
		DataOutputStream to = null;
		DataOutputStream toClientErr = null;
		String inputLine;
		try {
			ip = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println("Please enter your request, \"GET\" request only");// Just
																			// prompt
																			// user
																			// to
																			// enter
																			// request
			String protocol, host, path, query;
			int port;
			while ((inputLine = ip.readLine()) != null) {
				// try {
				// StringTokenizer tokens = new StringTokenizer(inputLine);
				// tokens.nextToken();
				// } catch (Exception e) {
				// break;
				// }
				//

				// Incomplete request line, error reported
				if (inputLine == null || inputLine.equals("")) {
					toClientErr = new DataOutputStream(socket.getOutputStream());
					String errorBody = "<html>\r\n"
							+ "<head>\r\n"
							+ "<title> 400 Bad Request Page </title>\r\n"
							+ "</head>\r\n"
							+ "<body>\r\n"
							+ "<h1> 400 Bad Request </h1>\r\n"
							+ "<hr>\r\n"
							+ "</body>\r\n"
							+ "</html>"
							+ "\r\n";
					int len = errorBody.length();
					String lens = Integer.toString(len);
					// Bad request message generator
					String header = "HTTP/1.0 " + BAD_REQUEST_CODE + " " + BAD_REQUEST_MESSAGE + "\r\n"
							+ "Content-type: text/html; charset=utf-8" + "\r\n" + "Content-length: " + lens + "\r\n"
							+ "Connection: Closed" + "\r\n\r\n";

					// toClientErr.writeBytes("Error Code " + BAD_REQUEST_CODE +
					// ": " + BAD_REQUEST_MESSAGE + "\n");
					toClientErr.writeBytes(errorBody);
					toClientErr.writeBytes(header);
					System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
					toClientErr.flush();
					toClientErr.close();
					throw new HTTPException(BAD_REQUEST_CODE);
					// throw new IOException("Please enter a conrrect request");
				}
				String[] tokens = inputLine.split(" ");
				// Also incomplete
				if (tokens.length < 3) {
					toClientErr = new DataOutputStream(socket.getOutputStream());
					String errorBody = "<html>\r\n"
							+ "<head>\r\n"
							+ "<title> 400 Bad Request Page </title>\r\n"
							+ "</head>\r\n"
							+ "<body>\r\n"
							+ "<h1> 400 Bad Request </h1>\r\n"
							+ "<hr>\r\n"
							+ "</body>\r\n"
							+ "</html>"
							+ "\r\n";
					int len = errorBody.length();
					String lens = Integer.toString(len);
					String header = "HTTP/1.0 " + BAD_REQUEST_CODE + " " + BAD_REQUEST_MESSAGE + "\r\n"
							+ "Content-type: text/html; charset=utf-8" + "\r\n" + "Content-length: " + lens + "\r\n"
							+ "Connection: Closed" + "\r\n\r\n";

					// toClientErr.writeBytes("Error Code " + BAD_REQUEST_CODE +
					// ": " + BAD_REQUEST_MESSAGE + "\n");
					toClientErr.writeBytes(errorBody);
					toClientErr.writeBytes(header);
					System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
					toClientErr.flush();
					toClientErr.close();
					throw new HTTPException(BAD_REQUEST_CODE);
				} else {
					String method = tokens[0];
					String uri = tokens[1];
					String version = tokens[2];

					if (!method.equals("GET")) {
						// All other HTTP methods, if the method is one of them,
						// 501 not implemented error reported
						if (method.equals("HEAD") || method.equals("POST") || method.equals("DELETE")
								|| method.equals("PUT") || method.equals("OPTIONS") || method.equals("TRACE")
								|| method.equals("CONNECT")) {
							toClientErr = new DataOutputStream(socket.getOutputStream());
							String errorBody = "<html>\r\n"
									+ "<head>\r\n"
									+ "<title> 501 Not Implemented Page </title>\r\n"
									+ "</head>\r\n"
									+ "<body>\r\n"
									+ "<h1> 501 Not Implemented </h1>\r\n"
									+ "<hr>\r\n"
									+ "</body>\r\n"
									+ "</html>"
									+ "\r\n";
							int len = errorBody.length();
							String lens = Integer.toString(len);
							String header = "HTTP/1.0 " + NOT_IMPLEMENTED_CODE + " " + NOT_IMPLEMENTED_MESSAGE + "\r\n"
									+ "Content-type: text/html; charset=utf-8" + "\r\n" + "Content-length: " + lens
									+ "\r\n" + "Connection: Closed" + "\r\n\r\n";

							// toClientErr.writeBytes("Error Code " +
							// BAD_REQUEST_CODE +
							// ": " + BAD_REQUEST_MESSAGE + "\n");
							toClientErr.writeBytes(errorBody);
							toClientErr.writeBytes(header);
							System.err.println("Error Code " + NOT_IMPLEMENTED_CODE + ": " + NOT_IMPLEMENTED_MESSAGE);
							toClientErr.flush();
							toClientErr.close();
							throw new HTTPException(NOT_IMPLEMENTED_CODE);

						} else {
							// If not, the request is a bad request
							toClientErr = new DataOutputStream(socket.getOutputStream());
							String errorBody = "<html>\r\n"
									+ "<head>\r\n"
									+ "<title> 400 Bad Request Page </title>\r\n"
									+ "</head>\r\n"
									+ "<body>\r\n"
									+ "<h1> 400 Bad Request </h1>\r\n"
									+ "<hr>\r\n"
									+ "</body>\r\n"
									+ "</html>"
									+ "\r\n";
							int len = errorBody.length();
							String lens = Integer.toString(len);
							String header = "HTTP/1.0 " + BAD_REQUEST_CODE + " " + BAD_REQUEST_MESSAGE + "\r\n"
									+ "Content-type: text/html; charset=utf-8" + "\r\n" + "Content-length: " + lens
									+ "\r\n" + "Connection: Closed" + "\r\n\r\n";

							// toClientErr.writeBytes("Error Code " +
							// BAD_REQUEST_CODE +
							// ": " + BAD_REQUEST_MESSAGE + "\n");
							toClientErr.writeBytes(errorBody);
							toClientErr.writeBytes(header);
							System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
							toClientErr.flush();
							toClientErr.close();
							throw new HTTPException(BAD_REQUEST_CODE);
						}
					}
					// Convert uri to url
					URL url = new URL(uri);
					host = url.getHost();// Try to get host
					// If host cannot be found, bad request error will be
					// reported
					if (host == null || host.equals("")) {
						toClientErr = new DataOutputStream(socket.getOutputStream());
						String errorBody = "<html>\r\n"
								+ "<head>\r\n"
								+ "<title> 400 Bad Request Page </title>\r\n"
								+ "</head>\r\n"
								+ "<body>\r\n"
								+ "<h1> 400 Bad Request </h1>\r\n"
								+ "<hr>\r\n"
								+ "</body>\r\n"
								+ "</html>"
								+ "\r\n";
						int len = errorBody.length();
						String lens = Integer.toString(len);
						String header = "HTTP/1.0 " + BAD_REQUEST_CODE + " " + BAD_REQUEST_MESSAGE + "\r\n"
								+ "Content-type: text/html; charset=utf-8" + "\r\n" + "Content-length: " + lens + "\r\n"
								+ "Connection: Closed" + "\r\n\r\n";

						// toClientErr.writeBytes("Error Code " +
						// BAD_REQUEST_CODE +
						// ": " + BAD_REQUEST_MESSAGE + "\n");
						toClientErr.writeBytes(errorBody);
						toClientErr.writeBytes(header);
						System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
						toClientErr.flush();
						toClientErr.close();
						throw new HTTPException(BAD_REQUEST_CODE);
					}
					port = url.getPort();
					protocol = url.getProtocol();
					path = url.getPath();
					// If not path is found, add /
					if (path == null || path.equals("")) {
						path = "/";
					}
					// When port is -1 and protocol is http, then set the port
					// to the default port which is 80
					if (port == -1) {
						if (protocol.equals("http")) {
							port = 80;// default port
						} else {
							// Else, 400 bad request error is reported
							toClientErr = new DataOutputStream(socket.getOutputStream());
							String errorBody = "<html>\r\n"
									+ "<head>\r\n"
									+ "<title> 400 Bad Request Page </title>\r\n"
									+ "</head>\r\n"
									+ "<body>\r\n"
									+ "<h1> 400 Bad Request </h1>\r\n"
									+ "<hr>\r\n"
									+ "</body>\r\n"
									+ "</html>"
									+ "\r\n";
							int len = errorBody.length();
							String lens = Integer.toString(len);
							String header = "HTTP/1.0 " + BAD_REQUEST_CODE + " " + BAD_REQUEST_MESSAGE + "\r\n"
									+ "Content-type: text/html; charset=utf-8" + "\r\n" + "Content-length: " + lens
									+ "\r\n" + "Connection: Closed" + "\r\n\r\n";

							// toClientErr.writeBytes("Error Code " +
							// BAD_REQUEST_CODE +
							// ": " + BAD_REQUEST_MESSAGE + "\n");
							toClientErr.writeBytes(errorBody);
							toClientErr.writeBytes(header);
							System.err.println("Error Code " + BAD_REQUEST_CODE + ": " + BAD_REQUEST_MESSAGE);
							toClientErr.flush();
							toClientErr.close();
							throw new HTTPException(BAD_REQUEST_CODE);
						}
					}
					query = url.getQuery();
					if (query != null && !query.equals("")) {
						path += "?" + query;

					}
					// If http version is not 1.0, then change to 1.0
					if (!version.equals("HTTP/1.0")) {
						version = "HTTP/1.0";
						System.out.println("HTTP version is changed");
					}
					Socket newSocket = new Socket(host, port);// New socket to
																// connect
																// server

					Socket toFilter = new Socket("hash.cymru.com", 43);

					lock.lock();
					try {
						to = new DataOutputStream(newSocket.getOutputStream());
						String toWrite = method + " " + path + " " + version + "\r\n" + "Host: " + host + "\r\n"
								+ "Connection: close\r\n\r\n"; // Generate
																// request
																// to remote
																// server
						to.writeBytes(toWrite);// Send request to server
						to.flush();
					} catch (Exception e) {
						System.err.println(e.getMessage());
					} finally {
						lock.unlock();
					}
					lock.lock();
					try {
						BufferedReader rd = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
						String line;
						StringBuilder response = new StringBuilder();
						while ((line = rd.readLine()) != null) {
							response.append(line + "\r\n");
						}
						String body[] = response.toString().split("\r\n\r\n");
						String toHash = body[1];
						String hashedText = "";
						try {
							MessageDigest m = MessageDigest.getInstance("MD5");
							m.reset();
							m.update(toHash.getBytes());
							byte[] digest = m.digest();
							BigInteger bigInt = new BigInteger(1, digest);
							hashedText = bigInt.toString(16);
							while (hashedText.length() < 32) {
								hashedText = "0" + hashedText;
							}
							hashedText.replaceAll(" ", "");
							hashedText += "\r\n";
						} catch (NoSuchAlgorithmException e) {
							System.err.println(e.getMessage());
						}
						DataOutputStream toFilterStream = new DataOutputStream(toFilter.getOutputStream());
						toFilterStream.writeBytes("\r\n" + hashedText);
					//	toFilterStream.writeBytes("\r\n" + "f40581e27c69d18f8c12c1297622866e" + "\r\n");

						BufferedReader rd2 = new BufferedReader(new InputStreamReader(toFilter.getInputStream()));

						StringBuilder responseFromFilter = new StringBuilder();
						String newLine;
						// response.append("\r\n");
						while ((newLine = rd2.readLine()) != null) {
							responseFromFilter.append(newLine);
						}
						String[] response2 = responseFromFilter.toString().split(" ");
						DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
						if (response2[response2.length - 1].equals("NO_DATA")) {
							response.insert(0, "\r\n");
							toClient.writeBytes(response.toString());
						} else {
							String malwareMessagePage = "<html>\r\n"
									+ "<head>\r\n"
									+ "<title> Malware massage page </title>\r\n"
									+ "</head>\r\n"
									+ "<body>\r\n"
									+ "<h1> Malware found !!! </h1>\r\n"
									+ "<hr>\r\n"
									+ "<p> The content was blocked because it is suspected of containing malware. </p>\r\n"
									+ "</body>\r\n"
									+ "</html>"
									+ "\r\n";
							String sendToClient = "\r\n" + body[0] + "\r\n\r\n" + malwareMessagePage;
							toClient.writeBytes(sendToClient);
						}
						rd2.close();
						rd.close();
						toClient.flush();
						toClient.close();
						to.close();
						toFilterStream.flush();
						toFilterStream.close();
						response.setLength(0);
						response.setLength(0);
					} catch (Exception e) {
						System.err.println(e.getMessage());
					} finally {
						lock.unlock();
					}
					try {
						if (newSocket != null)
							newSocket.close();
					} catch (IOException e) {
						System.err.println("Unable to close socket");
					}
					try {
						if (toFilter != null)
							toFilter.close();
					} catch (IOException e) {
						System.err.println("Unable to close socket");
					}
				}
			}
		} catch (Exception e) {
			if (e.getMessage() != null)
				System.err.println("Socket has been closed");
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
