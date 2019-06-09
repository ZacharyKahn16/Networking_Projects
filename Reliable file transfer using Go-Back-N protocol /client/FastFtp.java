/**
 * FastFtp Class
 *
 * CPSC 441
 * Assignment 3
 *
 * @author 	Zachary Kahn
 *
 * UCID: 10151534
 * Tutorial: T01
 *
 * Simplified FTP client based on UDP that provides reliable data transfer using
 * Go-Back-N. This class establishes a connection using a TCP handshake with a UDP
 * socket. A file is then sent to the server through the UDP socket. The main
 * thread handles sending the file contents, a ReceiverThread is created to receive
 * acknowledgements from the server, and a Timer thread is used if a timeout
 * occurs (due to a lost packet). All threads are synchronized and make use of
 * the Sement and TxQueue classes which are provided by the libftp.jar file.
 *
 */

package cpsc441.a3.client;

// Required imports.
import cpsc441.a3.shared.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;

public class FastFtp extends BasicFtp {

	// Necessary sockets and streams.
	private Socket socket = null;
	private DatagramSocket clientSocket = null;
	private DataOutputStream outputStream = null;
	private DataInputStream inputStream = null;
	// File (object) name, host name, and server port number.
	private String objectName;
	private String hostName = null;
	private int serverUDPPort;
	// Sequence number of packet being sent, queue to store packets in transit,
	// timer to handle timeouts from dropped packets, and timeout interval length.
	private int seqNum;
	private TxQueue queue = null;
	private Timer timeoutTracker = null;
	private long timeoutInterval;

	/**
	 * Constructor
	 * @param	window	Size of window (queue).
	 * @param timer 	Lengh of timeout interval (in milliseconds).
	 */
	public FastFtp(int window, int timer) {
		super(window, timer);
		this.queue = new TxQueue(window);
		this.timeoutInterval = timer;
	}

	/**
	 * Sends the specified file to the specified destination host:
	 * 1. send file infor over TCP
	 * 2. start receving thread to process coming ACKs
	 * 3. send file segment by segment
	 * 4. wait until txQueue is empty, i.e., all segments are ACKed
	 * 5. clean up (cancel timer, interrupt receving thread, close socket/files)
	 *
	 * @param serverName	Name of the remote server
	 * @param serverPort	Port number of the remote server
	 * @param fileName		Name of the file to be trasferred to the rmeote server
	 */
	@Override
	public void send(String serverName, int serverPort, String fileName) {

		// Object (file) name.
		this.objectName = fileName;
		// Host name.
		this.hostName = serverName;

		try {

			// Open TCP socket.
			socket = new Socket(serverName, serverPort);

			// Open UDP socket
			this.clientSocket = new DatagramSocket();

			// Create required streams.
			outputStream = new DataOutputStream(socket.getOutputStream());
			inputStream = new DataInputStream(socket.getInputStream());

			// Send the local UDP port number.
			System.out.println("Local Port Number: " + clientSocket.getLocalPort());
			outputStream.writeInt(clientSocket.getLocalPort());

			// Send the name of file to be transmitted.
			outputStream.writeUTF(fileName);

			// Send the length of the file to be transmitted.
			int fileExists = this.checkFileExists(fileName);
			if (fileExists == 0){
				System.out.println("Inputted file does not exist.");
				System.out.println("Terminating program.");
				return;
			}
			File file = new File(System.getProperty("user.dir") + "/" + fileName);
			outputStream.writeLong(file.length());

			outputStream.flush();

			// UDP port number of server for file transfer.
			this.serverUDPPort = inputStream.readInt();
			// Initial sequence number used by server.
			this.seqNum = inputStream.readInt();

			// Continue Sending until the end of the file is reached.
			FileInputStream fileReader = new FileInputStream(file);
			// Read in data from the file.
			byte[] fileData = new byte[Segment.MAX_PAYLOAD_SIZE];
			int read = fileReader.read(fileData);
			byte[] exactData = null;
			if (read > -1){
				exactData = Arrays.copyOf(fileData, read);
			}

			// Start ACK receiving thread.
			ReceiverThread receiverThread = new ReceiverThread(this);
			receiverThread.start();

			// Write data to client output stream.
			while (read > -1){
				// Create a Segment to to send.
				Segment nextPacket = new Segment(seqNum, exactData);
				// Increment the sequence number.
				this.seqNum++;

				// Wait until the queue is not full to send packets.
				while(this.queue.isFull()){
					Thread.yield();

					// If the receiverThread is accidentally destroyed, it needs to be
					// restarted. This is because if the receiver thread is not running
					// it will block all other threads and the program will not finish.
					if (!receiverThread.isAlive()){
						System.out.println("Error: ReceiverThread died unexpectedly.");
						System.out.println("Restarting ReceiverThread.");
						receiverThread = new ReceiverThread(this);
						receiverThread.start();
					}
				}
				this.processSend(nextPacket);
				read = fileReader.read(fileData);
				if (read > -1){
					exactData = Arrays.copyOf(fileData, read);
				}

			}

			// Wait until all packets have been ACKed.
			System.out.println("Wait until all packets have been ACKed.");
			while(!this.queue.isEmpty()){
				Thread.yield();
				if (!receiverThread.isAlive()){
					System.out.println("Error: ReceiverThread died unexpectedly.");
					System.out.println("Restarting ReceiverThread.");
					receiverThread = new ReceiverThread(this);
					receiverThread.start();
				}
			}

			// Cancel timer thread.
			System.out.println("Cancel Timer in main thread");
			this.timeoutTracker.cancel();
			this.timeoutTracker.purge();

			// Cancel receiver thread.
			receiverThread.processIsDone();
			// Have current thread yield so the ReceiverThread can terminate.
			// If current thread closes the clientSocket before the ReceiverThread
			// terminates, and IOException will be thrown in the ReceiverThread.
			// However, the exception does not impact the outcome of the program.
			Thread.yield();

		} catch (IOException e){
			UsefulHelpers.handleException(e, "IOException occurred in DataOutputStream or DataInputStream.");
			return;
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in DataOutputStream or DataInputStream.");
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
				// Close TCP socket.
				if (socket != null){
					socket.close();
				}
				// Close UDP socket.
				if (clientSocket != null){
					clientSocket.close();
				}
			} catch (Exception e){
				UsefulHelpers.handleException(e, "Could not properly close streams or sockets.");
				return;
			}
		}
	}

	/**
	 * Get the clientSocket.
	 *
	 * @return clientSocket The client UDP socket.
	 */
	public DatagramSocket getClientSocket(){
		return this.clientSocket;
	}

	/**
	 * Check if the requested file exists.
	 *
	 * @param filename	Name of the requested file.
	 * @return			1 if file exists, 0 otherwise.
	 */
	private int checkFileExists(String filename){
		// Get the file based on its name.
		File file = new File(System.getProperty("user.dir") + "/" + filename);

		// Check if the file exists and is a file.
		if (file.exists() && file.isFile()){
			return 1;
		}

		return 0;
	}

	/**
   * Send a Segment via the UDP socket to the server.
	 *
	 * @param seg Segment to send to the server.
	 */
	public synchronized void processSend(Segment seg) {
		try{
			// Get IP address of host and send packet to server.
			InetAddress IPAddress = InetAddress.getByName(this.hostName);
			DatagramPacket sendPacket = new DatagramPacket(seg.getBytes(),
				seg.getBytes().length, IPAddress, this.serverUDPPort);
			this.clientSocket.send(sendPacket);

			// add seg to the transmission queue
			boolean firstSeg = queue.isEmpty();
			this.queue.add(seg);

			// if this is the first segment in transmission queue, start the timer
			if (firstSeg){
				this.restartTimer();
			}
		} catch (UnknownHostException e){
			UsefulHelpers.handleException(e, "UnknownHostException occurred in processSend.");
			return;
		} catch (IOException e){
			UsefulHelpers.handleException(e, "IOException occurred in processSend.");
			return;
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in processSend.");
			return;
		}

	}

	/**
	 * Process a received ACK segment from the server.
	 *
	 * @param ack The ACKed segment sent from the server.
	 */
	public synchronized void processACK(Segment ack) {
		// if ACK not in the current window, do nothing
		Segment[] queueContents = this.queue.toArray();
		boolean containsACK = false;
		for (Segment s : queueContents){
			if (s.getSeqNum() < ack.getSeqNum()){
				containsACK = true;
				break;
			}
		}

		if (!containsACK){
			return;
		}

		try{
			// otherwise:
			// cancel the timer
			this.timeoutTracker.cancel();
			this.timeoutTracker.purge();
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in processACK while cancelling timer.");
			return;
		}

		try{
			// remove all segements that are acked by this ACK from the transmission queue
			for (Segment s : queueContents){
				if (s.getSeqNum() < ack.getSeqNum()){
					this.queue.remove();
				}
			}
		} catch (InterruptedException e){
			UsefulHelpers.handleException(e, "InterruptedException occurred in processACK while removing from queue.");
			return;
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in processACK while removing from queue.");
			return;
		}

		// if there are any pending segments in transmission queue, start the timer
		if (!this.queue.isEmpty()){
			this.restartTimer();
		}

	}

	/**
	 * If a timeout occurs, retransmit all the unACKed segments and restart the
	 * timer.
	 */
	public synchronized void processTimeout() {
		// get the list of all pending segments from the transmission queue
		Segment[] queueContents = this.queue.toArray();

		try{
			// go through the list and send all segments to the UDP socket
			InetAddress IPAddress = InetAddress.getByName(this.hostName);
			for (Segment s : queueContents){
				DatagramPacket sendPacket = new DatagramPacket(s.getBytes(),
					s.getBytes().length, IPAddress, this.serverUDPPort);
				this.clientSocket.send(sendPacket);
			}
		} catch (UnknownHostException e){
			UsefulHelpers.handleException(e, "UnknownHostException occurred in processTimeout.");
			return;
		} catch (IOException e){
			UsefulHelpers.handleException(e, "IOException occurred in processTimeout.");
			return;
		}  catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in processTimeout.");
			return;
		}

		// if there are any pending segments in transmission queue, start the timer
		if (!this.queue.isEmpty()){
			this.restartTimer();
		}

	}

	/**
	 * Restarts the timer after it has timed out or has been cancelled.
	 */
	public void restartTimer(){
		try{
			this.timeoutTracker = new Timer(true);
			this.timeoutTracker.schedule(new TimeoutHandler(this),
				this.timeoutInterval);
		} catch (IllegalStateException e){
			UsefulHelpers.handleException(e, "IllegalArgumentException occurred while trying to restart timer.");
			return;
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred while trying to restart timer.");
			return;
		}

	}

}
