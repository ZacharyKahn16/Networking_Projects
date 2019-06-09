/**
 * ReceiverThread Class
 *
 * @author 	Zachary Kahn
 *
 * Receives acknowledgements from the server and removes ACKed segments from
 * the queue by calling processACK.
 *
 */

package cpsc441.a3.client;

import cpsc441.a3.shared.*;
import java.io.*;
import java.net.*;
import java.lang.*;


public class ReceiverThread extends Thread {

  // Instance of the transmitter.
  private FastFtp transmitter;
  // Flag used to terminate thread.
  private boolean isDone = false;

  /**
   * Constructor
   * @param transmitter An instance of the transmitter that is sending the file
   *                    segments to the server and receiving acknowledgements.
   */
  public ReceiverThread(FastFtp transmitter){
    this.transmitter = transmitter;
  }

  /**
   * Receives a new segment from the server and calls processACK, which in turn
   * removes already ACKed segments from the queue.
   */
  public void run() {

    DatagramSocket clientSocket = this.transmitter.getClientSocket();

    // Kill thread from main once done.
    while(!this.isDone){
      try {
        // 1. receive a DatagramPacket pkt from UDP socket
        byte[] packetData = new byte[Segment.MAX_SEGMENT_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(packetData, packetData.length);
        clientSocket.receive(receivePacket);
        // 2. call processAck() in the main class to process pkt
        Segment ack = new Segment(receivePacket);
        this.transmitter.processACK(ack);

      } catch (IOException e){
        UsefulHelpers.handleException(e, "IOException occurred in ReceiverThread.");
        return;
      } catch (Exception e){
        UsefulHelpers.handleException(e, "Exception occurred in ReceiverThread.");
        return;
        }
      }
    }

/**
 * Used to terminate the receiver thread.
 */
public void processIsDone(){
  this.isDone = true;
  }
}
