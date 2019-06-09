/**
 * TimeoutHandler Class
 *
 * @author 	Zachary Kahn
 *
 * When the timer for the oldest unACKed segment times out all segments in the
 * queue must be retransmitted. This class calls the processTimeout method upon,
 * a timeout, which in turn retransmits the segments in the queue.
 *
 */

package cpsc441.a3.client;

import java.util.*;

class TimeoutHandler extends TimerTask {

  // Instance of the transmitter.
  private FastFtp transmitter = null;

  /**
   * Constructor
   * @param transmitter An instance of the transmitter that is sending the file
   *                    segments to the server and receiving acknowledgements.
   */
  public TimeoutHandler(FastFtp transmitter){
    this.transmitter = transmitter;
  }

  /**
   * Runs the processTimeout method upon a timeout.
   */
  @Override
  public void run(){
    this.transmitter.processTimeout();
  }
}
