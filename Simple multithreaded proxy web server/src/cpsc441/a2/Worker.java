/**
 * Worker Class
 *
 * @author 	Zachary Kahn
 *
 * Worker thread that performs the client request.
 * The worker has two modes, Web and Proxy Server mode.
 * In Proxy Server mode, the worker forwards the client's request to
 * the specified host and port and relays the response back to the client.
 * In Web Server mode, the worker searches for the requested file and sends
 * it back to the client. The worker thread also handles range requests.
 * The worker also checks if the client request is incorrectly formatted and if
 * the file requested exists.
 *
 */

package cpsc441.a2;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;

public class Worker implements Runnable {

	// Socket passed into worker.
	private Socket socket;


	/**
	 * Constructor
	 *
	 * @param socket	The client socket.
	 */
	public Worker(Socket socket){
		this.socket = socket;
	}

	/**
	 * Reads in the client request, determines if the worker should act in
	 * Proxy or Web Server mode, and sends the information requested
	 * (or error response) to the client.
	 */
	public void run(){

		// Necessary streams.
		DataOutputStream outputStream = null;
		BufferedInputStream inputStream = null;
		String clientRequest = null;


		try {
			// Create required streams.
			outputStream = new DataOutputStream(this.socket.getOutputStream());
			inputStream = new BufferedInputStream(this.socket.getInputStream());

			// Create request byte array.
			byte[] request = new byte[1024];

			// Read in the client request.
			inputStream.read(request);


			// Pattern we are looking to find in the data stream.
			byte[] sep = "\r\n\r\n".getBytes("UTF-8");

			// Index ahead of where the \r\n\r\n separator ends.
			int indexOfPayloadSeparator = UsefulHelpers.getIndexOfPayloadSeparator(sep, request);

			// If no \r\n\r\n found, send a 400 Bad Request to the client.
			if (indexOfPayloadSeparator == -1){
				System.out.println("Error: No \\r\\n\\r\\n separator found in client request message.");
				System.out.println("Terminating worker.");
				this.sendBadRequestToClient(outputStream);
				return;
			}

			clientRequest = new String(Arrays.copyOfRange(request, 0, indexOfPayloadSeparator));

			// Store key:value pairs of the information in the client request.
			HashMap<String, String> headerFields = UsefulHelpers.requestInfo(clientRequest);

			// Check if server should run in Web Server or Proxy Server mode.
			int mode = this.checkMode(headerFields);

			if(mode == -1){
				// Error occurred when trying to determine the hose,
				this.sendBadRequestToClient(outputStream);

			} else if (mode  == 1){
				System.out.println("In Web Server Mode");
				// Go into Web Server mode.
				this.webServerMode(outputStream, headerFields, new String(request, "UTF-8"));

			} else {
				System.out.println("In Proxy Server Mode");
				// Go into Proxy Server mode.
				this.proxyServerMode(outputStream, headerFields, request);
			}

		} catch (IOException e){
			UsefulHelpers.handleException(e, "IOException occurred in DataOutputStream or BufferedInputStream.");
			return;
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in DataOutputStream or BufferedInputStream.");
			return;
		} finally {
			try{
				// Close client input stream.
				if (inputStream != null){
					inputStream.close();
				}
				// Close client output stream.
				if (outputStream != null){
					outputStream.close();
				}
			} catch (Exception e){
				UsefulHelpers.handleException(e, "Could not properly close streams or socket in Proxy Server Mode.");
				return;
			}
		}
	}

	/**
	 * Check which mode the worker should run in based on the client
	 * request.
	 * 1 means the worker should run in Web Server mode.
	 * 0 means the worker should run in Proxy Server mode.
	 * -1 means the worker could not determine which mode it should run in,
	 * and so the worker will be terminated.
	 *
	 * @param headerFields	Hashmap containing information about the request.
	 * @return	Status that indicates which mode the worker runs in.
	 */
	private int checkMode(HashMap<String, String> headerFields){
		if (!headerFields.containsKey("Host")){
			// If host was not specified in the request, enter Web Server mode.
			return 1;
		}

		// Get the host and parse the port number out if it is included.
		String host = headerFields.get("Host").split(":")[0];
		boolean isLocalHost = false;
		// Check if host is the local host.
		try {
			isLocalHost = Utils.isLocalHost(host);
		} catch (UnknownHostException e){
			UsefulHelpers.handleException(e, "UnknownHostException occured when trying to determine server mode.");
			return -1;
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occured when trying to determine server mode.");
			return -1;
		}
		if (isLocalHost){
			return 1;
		}
		return 0;

	}

	/**
	 * Runs the worker in proxy server mode, which forwards the client
	 * request to specified socket and relays the response to the client.
	 *
	 * @param clientOutputStream	Client output stream where the response is sent.
	 * @param headerFields			Hashmap containing information about the request.
	 * @param clientReq				Original client request.
	 */
	private void proxyServerMode(DataOutputStream clientOutputStream,
			HashMap <String, String> headerFields, byte[] clientReq){

		// Necessary streams, file, and socket.
		DataOutputStream proxyOutputStream = null;
		BufferedInputStream proxyInputStream = null;
		Socket proxySocket = null;

		// Get the hostname and port number.
		String[] hostComponents = headerFields.get("Host").split(":");
		String hostname;
		int port;

		if (hostComponents.length > 1 ){
			// This means a port number was included, so use the port number provided.
			hostname = hostComponents[0];
			port = Integer.parseInt(hostComponents[1]);
		} else{
			// Use a default port number of 80.
			hostname = hostComponents[0];
			port = 80;
		}
		try{
			// Create socket.
			proxySocket = new Socket(hostname, port);

			// Create required streams.
			proxyOutputStream = new DataOutputStream(proxySocket.getOutputStream());
			proxyInputStream = new BufferedInputStream(proxySocket.getInputStream());


			// Forward client request.
			proxyOutputStream.write(clientReq);
			proxyOutputStream.flush();

			// Read in data from proxyInputStream.
			byte[] proxyData = new byte[16000];

			int read = proxyInputStream.read(proxyData);

			// Write data to client output stream.
			while (read > -1){
				clientOutputStream.write(proxyData, 0, read);
				read = proxyInputStream.read(proxyData);
			}

			// Flush the contents of the stream.
			clientOutputStream.flush();
		} catch (IOException e){
			UsefulHelpers.handleException(e, "IOException occurred in DataOutputStream or "
					+ "BufferedInputStream in Server Mode.");
			// Send a Bad Request to the client.
			this.sendBadRequestToClient(clientOutputStream);
			return;

		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in DataOutputStream or "
					+ "BufferedInputStream in Server Mode.");
			// Send a Bad Request to the client.
			this.sendBadRequestToClient(clientOutputStream);
			return;
		} finally {
			try{
				// Close socket.
				if (proxySocket != null){
					proxySocket.close();
				}
				// Close proxy input stream.
				if (proxyInputStream != null){
					proxyInputStream.close();
				}
				// Close proxy output stream.
				if (proxyOutputStream != null){
					proxyOutputStream.close();
				}
			} catch (Exception e){
				UsefulHelpers.handleException(e, "Could not properly close streams or socket in Proxy Server Mode.");
				return;
			}
		}
	}

	/**
	 * Checks that request is properly formatted.
	 *
	 * @param request	Client request.
	 * @return			1 if the request is properly formatted, 0 otherwise.
	 */
	private int properlyFormattedRequest(String request){
		// Check contain the request starts with GET or HEAD.
		if (!request.startsWith("GET") && !request.startsWith("HEAD")){
			return 0;
		}
		// Check that request is using HTTP/1.1.
		if (!request.contains("HTTP/1.1")){
			return 0;
		}

		// Request should contain \r\n\r\n
		if (!request.contains("\r\n\r\n")){ // CHECK if this is ok, or I should use ends with?
			return 0;
		}

		request = request.substring(0, request.indexOf("\r\n\r\n"));

		String[] lines = request.split("\n");
		for (int i=0; i<lines.length; i++){

			// The first line in the request should have exactly 3 parts.
			if (i == 0 && lines[i].split("\\s").length != 3){
				return 0;
			}

			// Every line besides the first should have the form Field: Info_for_field
			// and there should only be exactly one space character.
			if (i >0 && (!lines[i].replace("\n", "").replace("\r", "").matches(".+:\\s.+") ||
					lines[i].split("\\s").length != 2)){
				return 0;
			}
		}
		// In this case, the request is valid.
		return 1;
	}

	/**
	 * Check if the requested file exists.
	 *
	 * @param filename	Name of the requested file.
	 * @return			1 if file exists, 0 otherwise.
	 */
	private int checkFileExists(String filename){
		// Get the file based on its name.
		File file = new File(System.getProperty("user.dir") +filename);

		// Check if the file exists and is a file.
		if (file.exists() && file.isFile()){
			return 1;
		}

		return 0;
	}

	/**
	 * Checks if the request byte range if a valid range.
	 *
	 * @param file	Name of the requested file.
	 * @param start	Start of range.
	 * @param end	End of range.
	 * @return		1 if the range is valid, 0 otherwise.
	 */
	private int checkValidRangeRequest(File file, int start, int end){

		// A valid range means:
		// Start and end are both > 0.
		// Start and end are both < length of file.
		// Start < end.
		if ((start <0 || end < 0) || (start > file.length()-1 || end > file.length()-1) ||
				(start > end)){
			return 0;
		}

		return 1;
	}

	/**
	 * Generates the response header that is sent to the client.
	 *
	 * @param okReq			1 if the request is ok, 0 otherwise.
	 * @param statusCode	Either 200 OK, 400 Bad Request, or 404 Not Found.
	 * @param file			File requested.
	 * @param start			Starting byte of range requested.
	 * @param end			Ending byte of range requested
	 * @return				The header response.
	 */
	private String responseHeader(int okReq, String statusCode, File file,
			int start, int end){
		String response = null;

		// If the request was ok, include the headers required to be compliant with most
		// browsers.
		if (okReq == 1){
			response = "HTTP/1.1 200 OK\r\n";
			response = response + "Date: " + Utils.getCurrentDate() +"\r\n";
			response = response + "Server: MyAwesomeServer\r\n";
			response = response + "Last-Modified: " + Utils.getLastModified(file) +"\r\n";
			response = response + "Accept-Ranges: bytes\r\n";
			response = response + "Content-Length: " + file.length() + "\r\n";
			try{
				response = response + "Content-Type: " + Utils.getContentType(file) + "\r\n";
			} catch(Exception e){
				System.out.println("Warning: Could not get content type of the file.");
			}

			// Indicate the range of bytes returned for valid range requests.
			if (start > -1 && end > -1){
				response = response + "Content-Range: bytes "+ start + "-" + end +"/"+ file.length() +"\r\n";
			}

		} else {
			// This means the request was either a 400 404 error.
			// Only the Date and Server header fields are required for error responses.
			response = "HTTP/1.1 "+ statusCode +"\r\n";
			response = response + "Date: " + Utils.getCurrentDate() +"\r\n";
			response = response + "Server: MyAwesomeServer\r\n";
		}

		response = response + "Connection: close\r\n\r\n";

		return response;
	}

	/**
	 * Runs the worker in Web Server mode, which generates the header
	 * response, reads the file in (if it exists and the request was properly
	 * formatted), and sends the entire file (or requested range) to the client.
	 *
	 * @param clientOutputStream	Client output stream where the response is sent.
	 * @param headerFields			Hashmap containing information about the request.
	 * @param clientReq				Original client request.
	 */
	private void webServerMode(DataOutputStream clientOutputStream,
			HashMap <String, String> headerFields, String clientRequest){

		// Check if the request is properly formatted.
		int properFormat = this.properlyFormattedRequest(clientRequest);

		// Determine if requested object exists.
		String objectName = headerFields.get("requestLine").split("\\s")[1];
		int fileExists = this.checkFileExists(objectName);

		// Necessary stream.
		FileInputStream webInputStream = null;

		try{
			String response;
			// Check if the header request is properly formatted and if the file exists.
			if (properFormat == 0 || fileExists == 0){
				if (properFormat == 0){
					// Send a 400 Bad Request response.
					response = this.responseHeader(0, "400 Bad Request", null,
							-1, -1);
				} else {
					// Send a 404 Not Found response.
					response = this.responseHeader(0, "404 Not Found", null,
							-1, -1);
				}

				// Send the response to the client and terminate the worker.
				clientOutputStream.write(response.getBytes());
				clientOutputStream.flush();

				return;
			}

			//  Get the requested file.
			File file = new File(System.getProperty("user.dir")+ objectName);
			// If range request was made, check it is in a valid range.
			int start = -1;
			int end = -1;
			if (headerFields.containsKey("Range")){
				String range = headerFields.get("Range").split("=")[1];
				start = Integer.parseInt(range.split("-")[0]);
				end = Integer.parseInt(range.split("-")[1]);
				int isValid = this.checkValidRangeRequest(file, start, end);

				if (isValid == 0){
					// If an invalid range was requested, response with a 400 Bad Request message.
					this.sendBadRequestToClient(clientOutputStream);
					return;
				}

			}

		// Get the response header for a 200 OK response.
		response = this.responseHeader(1, "200 OK", file, start, end);
		clientOutputStream.write(response.getBytes());
		clientOutputStream.flush();
		// Stop the worker thread here if request was a HEAD request.
		if (clientRequest.startsWith("HEAD")){
			return;
		}
			// Create required streams.
			webInputStream = new FileInputStream(file);

			// Read in data from proxyInputStream.
			byte[] fileData = new byte[4096];

			// If a range request was made, start reading from the first byte requested.
			// Recall that the range of the request has already been confirmed to be valid.
			if (start > -1){
				webInputStream.skip(start);
			}

			// Read in data from the file.
			int read = webInputStream.read(fileData);

			// If no range request was made, send the entire file to the client.
			if (start == -1 && end == -1){
				// Write data to client output stream.
				while (read > -1){
					clientOutputStream.write(fileData, 0, read);
					read = webInputStream.read(fileData);
				}

			} else { // A range request has been made.
				int cur = start;
				// Continue reading in the file until the requested range of bytes have
				// been sent to the client.
				while (cur < end && read >-1){
					int t = end - cur +1;
					if (read < end - cur + 1){
						clientOutputStream.write(fileData, 0, read);
					} else{
						clientOutputStream.write(fileData, 0, end - cur + 1);
					}
					read = webInputStream.read(fileData, 0, read);

					// Keep track of how many bytes have been read in.
					cur += read;

				}
			}

			// Flush the contents of the stream.
			clientOutputStream.flush();
		} catch (IOException e){ // Handle IO exceptions.
			UsefulHelpers.handleException(e, "IOException occurred in DataOutputStream or "
					+ "FileInputStream in Web Server Mode.");
			return;

		} catch (Exception e){ // Handle all other exceptions.
			UsefulHelpers.handleException(e, "General exception occurred in DataOutputStream or "
					+ "FileInputStream in Web Server Mode.");
			return;
		} finally {
			try{
				// Close web input stream.
				if (webInputStream != null){
					webInputStream.close();
				}
			} catch (Exception e){
				UsefulHelpers.handleException(e, "Could not properly close streams or socket in Proxy Server Mode.");
				return;
			}
		}
	}

	/**
	 * Used to send a bad request to the client.
	 *
	 * @param clientOutputStream	Client output stream where the response is sent.
	 */
	private void sendBadRequestToClient(DataOutputStream clientOutputStream){
		String response = this.responseHeader(0, "400 Bad Request", null,
				-1, -1);
		// Send the response to the client and terminate the worker.
		try{
			clientOutputStream.write(response.getBytes());
			clientOutputStream.flush();
		} catch (Exception e2){
			UsefulHelpers.handleException(e2, "Could not send Bad Request message to client.");
		}
	}
}
