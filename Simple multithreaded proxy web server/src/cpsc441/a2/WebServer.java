

/**
 * WebServer Class
 *
 * The main thread that listens for connection requests.
 * As new requests are accepted from a client, a new worker thread is made
 * to handle the connection.
 *
 */

package cpsc441.a2;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;


public class WebServer extends BasicWebServer {

	private boolean shutdown = false;
	private final int POOL_SIZE = 8;

	// Call the parent constructor
	public WebServer(int port) {
		super(port);
	}

	// Start the server
	public void run() {

		// Server socket.
		ServerSocket serverSocket = null;


		try {
			// Open the server socket.
			serverSocket = new ServerSocket(serverPort);
			// Set socket timeout.
			serverSocket.setSoTimeout(1000);
		} catch (IOException ioe) {

			UsefulHelpers.handleException(ioe, "IO Exception when trying to instantiate ServerSocket.");

		} catch (IllegalArgumentException e){
			UsefulHelpers.handleException(e, "Port number is outside allowed range.");
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General Exception when trying to instantiate ServerSocket.");
		}

		// Create POOL_SIZE threads to handle incoming requests.
		ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

		while (!shutdown){
			try {
				// Accept request.
				Socket socket = serverSocket.accept();

				// Let worker thread handle the request.
				Worker worker = new Worker(socket);
				executor.execute(worker);
			} catch (SocketTimeoutException e){
				// Used to allow process to check the shutdown status flag.
			} catch (Exception e){
				System.out.println("Error occurred while listening for "
						+ "a connection to be made to the server socket.");
			}
		}

		// --------------------------------------------------------- //

		// Shutdown the executor.
		try {
			// Do not accept any new tasks.
			executor.shutdown();
			// Wait 5 seconds for existing tasks to terminate.
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow(); // cancel currently executing tasks
			}
		} catch (InterruptedException e) {
			// cancel currently executing tasks
			executor.shutdownNow();
		}

		// --------------------------------------------------------- //
	}

	// shutdown the server
	public void shutdown() {
		shutdown = true;
	}

}
