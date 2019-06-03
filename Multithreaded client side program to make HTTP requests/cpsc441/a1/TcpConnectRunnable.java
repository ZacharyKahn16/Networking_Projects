
/**
 * TcpConnectRunnable
 * Implements the run() method for the Runnable interface.
 * This class establishes a TCP connection, and makes a GET request
 * for the given host, path, and range requested (if applicable).
 * Once the GET request is made, its contents are stored in a
 * temporary file.
 * 
 * @author Zachary Kahn
 * 
 */
package cpsc441.a1;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.io.IOException;


public class TcpConnectRunnable implements Runnable {

	private String hostname;
	private int port;
	private String pathname;
	private int start;
	private int end;
	private int threadID;
	private String filename;
	
	/**
	 * Constructor
	 * 
	 * @param hostname	
	 * @param port
	 * @param pathname
	 * @param start		Start of the requested range.
	 * @param end		End of the requested range.
	 * @param threadID	ID of the thread.
	 * @param filename	
	 */
	public TcpConnectRunnable(String hostname, int port, String pathname,
			int start, int end, int threadID, String filename){

		this.hostname = hostname;
		this.port = port;
		this.pathname = pathname;
		this.start = start;
		this.end = end;
		this.threadID = threadID;
		this.filename = filename;
		
	}
	
	/**
	 * Use to located the index of the payload (\r\n\r\n)
	 * separator within the GET request.
	 * 
	 * @param pattern	The pattern being searched.
	 * @param data		The data that contains the potential pattern.
	 * @return			Index of where the pattern ends.
	 */
	public int getIndexOfPayloadSeparator(byte[] pattern, byte[] data){
		
		int index = -1;
		// Search through all the data bytes.
		for (int i=0; i< data.length; i++){
			if (i+pattern.length >= data.length){
				// This means that pattern was not found in the array.
				return -1;
			}
			int j;
			// Check if data[i...i+j] == pattern byte array
			for (j=0; j< pattern.length; j++){
				if (data[i+j] != pattern[j]){
					// This means the pattern was not found.
					break;
				}
			}
			
			if (j == pattern.length){
				// This means the pattern was found.
				index = i + j;
				break;
			}
		}
		
		return index;
		
	}
	
	/**
	 * Makes GET request and stores the contents retrieved
	 * in a temporary file.
	 */
	public void run(){
		// Necessary streams, file, and socket.
		DataOutputStream outputStream = null;
		BufferedInputStream inputStream = null;
		Socket socket = null;
		FileOutputStream tempFile = null;
		
		try{
			// Create socket.
			socket = new Socket(this.hostname,this.port);
			
			
			// Create required streams.
			outputStream = new DataOutputStream(socket.getOutputStream());
			inputStream = new BufferedInputStream(socket.getInputStream());
			
			// Send GET request to server
			String getReq = "GET " + this.pathname +" HTTP/1.1\r\n";
			getReq = getReq + "Host: " + this.hostname +"\r\n";
			// Check if a valid range was given.
			if (this.start > -1 && this.end > -1){
				getReq = getReq + "Range: bytes=" + Integer.toString(this.start) + "-" + Integer.toString(this.end) + "\r\n\r\n";
			} else{ // If invalid range given, make a GET request that request the entire object.
				getReq = getReq + "\r\n\r\n";
			}
			
			
			// Send request to server
			byte[] getRequestInByes = getReq.getBytes("UTF-8");
			outputStream.write(getRequestInByes);
			outputStream.flush();
			
			// Store chunks of the requested data.
			byte[] data = new byte[4096];
			
			// Read in data from the input stream.
			int bytesRead = inputStream.read(data);
			
			// Pattern we are looking to find in the data stream.
			byte[] sep = "\r\n\r\n".getBytes("UTF-8");
			
			// Index ahead of where the \r\n\r\n separator ends.
			int indexOfPayloadSeparator = this.getIndexOfPayloadSeparator(sep, data);
			int offset;
			if (indexOfPayloadSeparator == -1){ // Pattern was not found.
				System.out.println("Warning: Payload separator \\r\\n\\r\\n was not found.");
				offset = 0;
			} else{ // Pattern was found.
				offset = indexOfPayloadSeparator;
			}
			
			// Creating temporary file to store data from GET request.
			tempFile = new FileOutputStream(this.filename + "tmp" + this.threadID);
			// Only write the data retrieved into the temporary file, not any of the
			// header information.
			tempFile.write(data, offset, bytesRead-offset);
			
			bytesRead = inputStream.read(data);
			
			// Read in the rest of the data from the stream.
			while (bytesRead > -1){	
    			tempFile.write(data, 0, bytesRead);	
        		bytesRead = inputStream.read(data);
        	}
			
			tempFile.flush();

		} catch (IOException ioe){ // IO error catching.
			System.out.println("IO Exception occurred.");
			System.out.println("Description of error: "+ ioe.getMessage());
			System.exit(-1);
			
		} catch(Exception e){ // General error catching.
			System.out.println("Error: " + e.getMessage());
			System.exit(-1);
		} finally {
			
			try {
				// Close the file.
				if (tempFile != null){
					tempFile.close();
				}
				// Close the open streams.
				if (inputStream != null){
					inputStream.close();
				}
				
				if (outputStream != null){
					outputStream.close();
				}
				// Close the socket.
				if (socket != null){
					socket.close();
				}	
				
			} catch (Exception e){
				System.out.println("Could not close one of the streams, socket, or created files.");
				System.exit(-1);
			}
		}
	}
}
