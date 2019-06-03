/**
 * QuickUrl
 * Makes a HEAD request followed by a GET request to obtain the contents
 * located at a given url. The number of threads to be used to obtain
 * all the data can be specified, and will be used if range requests are
 * supported. The contents obtained are written to a file in the working
 * directory with the same name as the file given in the url.
 * 
 * @author Zachary Kahn
 * 
 */
package cpsc441.a1;

import java.net.Socket;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;

public class QuickUrl extends ConcurrentHttp {

	/**
	 * From assignment:
	 * This is the main method for downloading objects. The parameter url specifies a properly
	 * formatted URL that specifies the object to be downloaded.
 	 * If the server supports Range requests, the specified object is split into conn parts,
 	 * where all parts are download concurrently using conn threads.
	 */
	public void getObject(String url) {
		DataOutputStream outputStream = null;
		BufferedInputStream inputStream = null;
		HeadResponse res = null;
		Socket socket = null;
		UrlParser urlParse = null;
		
		// Put more comments.
		// Check if file exists. If it does, override it.
		
		try{
			// Parse url.
			urlParse = new UrlParser(url);
			urlParse.determineHostnameAndPathName();
			
			// Open socket.
			socket = new Socket(urlParse.getHostname(),urlParse.getPort());
			
			// Create required streams.
			outputStream = new DataOutputStream(socket.getOutputStream());
			inputStream = new BufferedInputStream(socket.getInputStream());
			
			// Send HEADER request to server.
			String headerReq = "HEAD " + urlParse.getPath() +" HTTP/1.1\r\n" + "Host: " + urlParse.getHostname() +"\r\n\r\n";
				
			// Send request to server.
			byte[] headerRequestInByes = headerReq.getBytes("UTF-8");
			outputStream.write(headerRequestInByes);
			outputStream.flush();

			// Save the HEADER response.
			byte[] headerResponseBytes = new byte[1024];
		
			// Read in the HEADER response.
			inputStream.read(headerResponseBytes);
			
			// Get the HEADER response as a String.
			String headerResponseString = new String(headerResponseBytes, "UTF-8");
			
			// Used to check if a valid header response was received.
			res = new HeadResponse(headerResponseString);	
			
		} catch (IOException ioe){
			// Catch IO exceptions.
			System.out.println("IO Exception occurred.");
			System.out.println("Description of error: "+ ioe.getMessage());
			System.out.println("Terminating program.");
			System.exit(-1);
			
		} catch(Exception e){
			// Catch all other exceptions.
			System.out.println("Error: " + e.getMessage());
			System.out.println("Terminating program.");
			System.exit(-1);
		} finally {
			try {
				// Close streams.
				if (inputStream != null){
					inputStream.close();
				}
				
				if (outputStream != null){
					outputStream.close();
				}
				// Close socket.
				if (socket != null){
					socket.close();
				}	
				
			} catch (Exception e){
				System.out.println("Could not close one of the streams or the socket.");
				System.exit(-1);
			}
		}
		
		// File where all results will be stored.
		FileOutputStream fullResponseFile = null;
		
		try {
			// Get the range of bytes in the response.
			int range = res.getRange();
			// Initialize the interval of each request.
			int contentRange = 1;
			// Get the file name of the file.
			String filename = urlParse.getFileName();
			if (range == -1){
				// This means range requests were not available. Therefore, only use one thread.
				this.setConn(1);
			} else {
				// Check if connections requested is greater than the range.
				if (this.getConn()> range){
					this.setConn(range);
				} else{
					// The range request interval is equal to range/(number of threads)
					contentRange = range/this.getConn();
				}	
			}
			
			// Create conn threads.
			ExecutorService executor = Executors.newFixedThreadPool(this.getConn());
			
			int start;	// Start index of current range request.
			int end;	// End index of current range request.
			
			
			if (this.getConn() <= 1){
				// Start and end set to -1 indicates to only use 1 thread.
				start = -1;
				end = -1;
			} else{
				// Initial range request indices.
				start = 0;
  				end = contentRange - 1;
			}
			// Get the host name, port number, and pathname from the url.
			String hostname = urlParse.getHostname();
			int port = urlParse.getPort();
			String pathname = urlParse.getPath();
			
			// Go through all threads.
			for (int i= 0; i<this.getConn(); i++){
				
				// Create a new worker.
				TcpConnectRunnable worker = new TcpConnectRunnable(hostname, port, pathname,
						start, end, i, filename);
				executor.execute(worker);
				
				// Set the new start and end range for the next thread.
				start = end +1;
				if (i < this.getConn() -2){
					end += contentRange;
				} else{
					// The last thread should read the remaining bytes left to read.
					// The reason this could occur is because the Content-Length % numThreads might
					// have equal 0, in which case we need to threadRange + Content-Length % numThreads
					// bits.
					end = range -1;
				}
				
			}
			
			executor.shutdown();
			executor.awaitTermination(20, TimeUnit.SECONDS);
			
			System.out.println("All threads finished.");
			 
			// Create an array to store all the temporary files that were created by the workers.
			String[] tempFiles = new String[this.getConn()];
			int i =0;
			
			// Get the local directory.
			String localDir = new File( "." ).getCanonicalPath();
			
			// Get a list of the temporary files created.
	        File folder = new File(localDir);
	        String[] files = folder.list();
	        
	        for (String f: files){
	        	
	        	if (f.startsWith(filename + "tmp")){
	        		tempFiles[i] = f;
	        		i++;
	        	}
	        }
	        
	        // Make sure files are sorted by thread ID.
	        Arrays.sort(tempFiles, new Comparator<String>(){
	        	public int compare(String f1, String f2){
	        		// The +3 is added since the name of the temporary file is
	        		// the file name from the url concatenated with "tmp".
	        		int f1Name = Integer.parseInt(f1.substring(filename.length()+3));
	        		int f2Name = Integer.parseInt(f2.substring(filename.length()+3));
	        		
	        		if (f1Name -f2Name >=0){
	        			return 1;
	        		} else{
	        			return -1;
	        		}
	        	}
	        });
	        
	        // Write the contents of each temporary file to one common file.
	        fullResponseFile = new FileOutputStream(new File(localDir + "/"+ filename));
	        File tmpFile = null;
	        
	        byte[] buff= new byte[4096];
	        int r = -1;
	        
	        for (String file: tempFiles){
	        	// Read in each temporary file.
	        	tmpFile = new File(localDir + "/"+ file);
	        	FileInputStream curFile = new FileInputStream(tmpFile);
	        	r = curFile.read(buff);
	        	while (r > -1){
	        		fullResponseFile.write(buff, 0, r);	
	        		r = curFile.read(buff);
	        	}
	        	
	        	fullResponseFile.flush();
	        	// Close the temporary file.
	        	curFile.close();
	        	
	        	// Delete the temporary file.
	        	tmpFile.delete();
	        	
	        	System.out.println("File "+ file + " has been read in and re-written.");
	        }
	        
 
		} catch (IOException ioe){ // IO error catching.
			System.out.println("IO Exception occurred.");
			System.out.println("Description of error: "+ ioe.getMessage());
			System.out.println("Terminating program.");
			System.exit(-1);		
			
		} catch(Exception e){ // General error catching.
			System.out.println("Error: " + e.getMessage());
			System.out.println("Terminating program.");
			System.exit(-1);
		} finally {
			try {
				// Close the file.
				if (fullResponseFile != null){
					fullResponseFile.close();
				}	
				
			} catch (Exception e){
				System.out.println("Could not close file.");
				System.exit(-1);
			}
		}
	}
	
	
}