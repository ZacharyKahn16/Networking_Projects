

/**
 * BasicFtp Class
 *
 * This class must be extended by class FastFtp.
 * The main mehtod to be implemented in FastFtp is the method send().
 * 
 * @author 	Majid Ghaderi
 * @version	2019
 *
 */

package cpsc441.a3.client;

public abstract class BasicFtp {

	private int windowSize;
	private int rtoTimer;

    /**
     * Constructor to initialize the program
     * You may override this method if needed
     *
     * @param window	Size of the window for Go-Back_N (in segments)
     * @param timer		The time-out interval for the retransmission timer (in milli-seconds)
     */
	public BasicFtp(int window, int timer) {
		windowSize = window;
		rtoTimer = timer;
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
	public abstract void send(String serverName, int serverPort, String fileName);

}
