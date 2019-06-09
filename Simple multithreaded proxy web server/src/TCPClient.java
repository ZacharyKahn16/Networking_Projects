import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A Simple Client used to test web server.
 */

public class TCPClient {

	public static void main(String[] args) {

		DataOutputStream outputStream = null;
		BufferedInputStream inputStream = null;
		Socket socket = null;


		try{

			// Open socket.
			socket = new Socket("localhost", 2525);

			// Create required streams.
			outputStream = new DataOutputStream(socket.getOutputStream());
			inputStream = new BufferedInputStream(socket.getInputStream());

			// Send HEADER request to server.
//			String headerReq = "HEAD /~mghaderi/cpsc441/dots.txt HTTP/1.1\r\nHost: people.ucalgary.ca\r\nRange: bytes=0-19\r\n";
//			String headerReq = "GET /nums.txt HTTP/1.1\r\nHost: localhost\r\nRange: bytes=2500-9000\r\n\r\n";
			String headerReq = "GET /dts.txt HTTP/1.1\r\nRange: bytes=0-19\r\n\r\n";

			// Send request to server.
			byte[] headerRequestInByes = headerReq.getBytes("UTF-8");
			outputStream.write(headerRequestInByes);
			outputStream.flush();

			// Save the HEADER response.
			byte[] headerResponseBytes = new byte[16000];

			// Read in the HEADER response.
			int read = inputStream.read(headerResponseBytes);
			while(read>-1){
				// Get the HEADER response as a String.
				String headerResponseString = new String(headerResponseBytes, "UTF-8");

				System.out.println(headerResponseString);

				read = inputStream.read(headerResponseBytes);
			}



		} catch (Exception ioe){
			// Catch IO exceptions.
			System.out.println("IO Exception occurred.");
			System.out.println("Description of error: "+ ioe.getMessage());
			System.out.println("Terminating program.");
			System.exit(-1);

		}
	}
}
