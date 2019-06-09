/**
 * Router Class
 *
 * @author 	Zachary Kahn
 *
 * A router with a unique name listens on a specified port number of other
 * neighbors in the network. The router is configured to broadcast to all links
 * that connect to it, and will periodically broadcast its local DV to let
 * its neighbors know it is alive. To learn about other nodes in the network
 * the Bellman-Ford algorithm is implemented. If neighboring nodes become
 * inactive, they are removed from the local DV table. The maximum allowable
 * cost to a node in the network is defined by BasicRouter.COST_INFINITY. A node
 * at or above this cost is considered to be an inactive node within the network
 * and so it is removed from the local DV and forwarding table.
 *
 * The local DV stores the cost to get to other nodes in the network.
 * Ex. Local router = r0, DV[r3] = 3
 *  This means the minimum cost path from r0 to r3 is 3.
 * The local forwarding table stores the next hop router to get to a specific
 * node.
 * Ex. Local router = r0, FT[r3] = r1
 *  This means to go from r0 to r3 go through r1 (which is a neighbor of r0).
 *
 */


package cpsc441.a4.router;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Router extends BasicRouter {

  // Local DV, forwarding table, and DV table.
  private ConcurrentHashMap<String, Integer> localDV = null;
  private ConcurrentHashMap<String, String> forwardingTable = null;
  private ConcurrentHashMap<String, Map<String, Integer>> dvTable = null;
  // List of neighboring nodes.
  private ArrayList<String> neighbors = null;
  // Flag to check if local DV was updated and need to be broadcasted.
  private int updateMade = 0;
  // Broadcast socket.
  private DatagramSocket socket = null;
  // Flag to terminate Router.
  private int isShutdown = 0;
  // Schedulers to monitor inactive timers and send keep alive messages.
  private ScheduledExecutorService monitorActivityPool = null;
  private ScheduledExecutorService keepRouterAlivePool = null;
  // List of future tasks scheduled.
  private ConcurrentHashMap<String, ScheduledFuture<?>> futureTasks = null;
  private ScheduledFuture keepRouterAlive = null;


  /**
	 * Constructor to initialize the rouer instance
	 *
	 * @param routerName			Unique name of the router
	 * @param routerPort			UDP port number used by the router for sending/receiving routing messages
	 * @param keepaliveInterval		Time interval for sending keepalive beacons to neighboring routers (in milli-seconds)
	 * @param inactivityInterval	Time interval to purge inactive neighbors (in milli-seconds)
	 */
	public Router(String routerName, int routerPort, int keepaliveInterval, int inactivityInterval) {
    super(routerName, routerPort, keepaliveInterval, inactivityInterval);
    this.localDV = new ConcurrentHashMap<String, Integer>();
    this.localDV.put(routerName, 0);
    this.forwardingTable = new ConcurrentHashMap<String, String>();
    this.forwardingTable.put(routerName, routerName);
    this.dvTable = new ConcurrentHashMap<String, Map<String, Integer>>();
    this.neighbors = new ArrayList<String>();
    this.futureTasks = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	}



	/**
	 * Starts the router
	 *
	 * This is the main method that implements the DV algorithm.
	 */
	public void run(){

    try{
      // Setup a UDP broadcast socket.
      System.out.println("Initial broadcast.");
      this.socket = new DatagramSocket(this.port);
      this.socket.setBroadcast(true);

      // Broadcast initial DV.
      this.broadcastLocalDV();

      // Create an inactivity timer pool.
      this.monitorActivityPool = Executors.newScheduledThreadPool(8);

      // Create an keep alive timer pool.
      this.keepRouterAlivePool = Executors.newScheduledThreadPool(1);

      // Start the keep alive timer.
      System.out.println("Starting keepalive timer.");
      this.keepRouterAlive = this.keepRouterAlivePool.schedule(
        new KeepaliveTimer(this),this.keepalive, TimeUnit.MILLISECONDS);

      DatagramPacket receivePacket = null;

      // While not shutdown.
      while (this.isShutdown == 0){

        try{
          // Data received should be less than 64000 bytes.
          byte[] receiveData = new byte[64000];
          // Receive a packet from a neighbor.
          receivePacket = new DatagramPacket(receiveData, receiveData.length);
          this.socket.receive(receivePacket);

          // Process the received DV.
          DvPacket receivedDvPacket = new DvPacket(receivePacket);
          String neighborRouter = receivedDvPacket.getSource();
          Map<String, Integer> neighborDistVector = receivedDvPacket.getDvTable();
          System.out.println("Receive DV[" + neighborRouter + "]: " + neighborDistVector);

          // Add undiscovered neighbor to neighbor list.
          int updatedRequired = 0;
          int restartInactivityTimerRequired = 1;
          // Make sure that the local DV is not added as a neighboring router.
          if (!neighborRouter.equals(this.name)){
            if (!this.neighbors.contains(neighborRouter)){
              // Add new router to list of neighboring routers.
              this.neighbors.add(neighborRouter);
              // Add new neighbor's DV to DvTable.
              this.dvTable.put(neighborRouter, neighborDistVector);

              // Start an inactivity timer for new neighbor.
              ScheduledFuture<?> newTimer = this.monitorActivityPool.schedule(
                new InactivityTimer(this, neighborRouter),this.inactivity,
                TimeUnit.MILLISECONDS);

              // Add timer for new neighbor to map of future tasks.
              this.futureTasks.put(neighborRouter, newTimer);

              // An update for the local DV is now required and there is no need
              // to restart the inactivitiy timer.
              updatedRequired = 1;
              restartInactivityTimerRequired = 0;
            } else if (!this.dvTable.get(neighborRouter).equals(neighborDistVector)){
              // If the received DV is different from the stored one, update it.
              this.dvTable.replace(neighborRouter, neighborDistVector);
              updatedRequired = 1;
            }
          } else {
	           restartInactivityTimerRequired = 0;
	        }

          // Restart inactivity timer if this is not a new neighbor.
          if (restartInactivityTimerRequired == 1){
            System.out.println("Restart inactivity timer for " + neighborRouter);
            this.futureTasks.get(neighborRouter).cancel(true);

            // Start an inactivity timer for new neighbor.
            ScheduledFuture<?> newTimer = this.monitorActivityPool.schedule(
              new InactivityTimer(this, neighborRouter),this.inactivity,
              TimeUnit.MILLISECONDS);

            // Add timer for new neighbor to map of future tasks.
            this.futureTasks.replace(neighborRouter, newTimer);
          }

          if (updatedRequired == 1){
            System.out.println("Update required for " + neighborRouter);
            // Add destinations in neighbor's DV to local DV.
            for (String dest : neighborDistVector.keySet()){
              if (!this.localDV.containsKey(dest)){
                // Initialize new destination cost as COST_INFINITY.
                this.localDV.put(dest, BasicRouter.COST_INFINITY);
                // To get to this new destination, pass through the router where
                // you discovered this new destination.
                this.forwardingTable.put(dest, neighborRouter);
              }
            }
            // Update the local DV.
            this.updateLocalDV();
          }

          // Local DV has changed.
          if (this.updateMade == 1){
            // Broadcast local DV and restart the keepalive timer.
            this.sendKeepaliveMessage();
          }
          // Reset the updateMade flag.
          this.updateMade = 0;
        } catch (IOException e){
    			UsefulHelpers.handleException(e, "IOException occurred in while loop.");

    		} catch (Exception e){
    			UsefulHelpers.handleException(e, "General exception occurred in while loop.");
        }
      }


    } catch (IOException e){
			UsefulHelpers.handleException(e, "IOException occurred in DataOutputStream or DataInputStream.");
			return;
		} catch (Exception e){
			UsefulHelpers.handleException(e, "General exception occurred in DataOutputStream or DataInputStream.");
			return;
		} finally {
			try{
        // Shutdown the activity pool scheduler.
        this.monitorActivityPool.shutdown();
        this.monitorActivityPool.awaitTermination(1, TimeUnit.SECONDS);

        // Shutdown the keepalive scheduler.
        this.keepRouterAlivePool.shutdown();
        this.keepRouterAlivePool.awaitTermination(1, TimeUnit.SECONDS);

				// Close UDP socket.
				if (socket != null){
					this.socket.close();
				}
			} catch (Exception e){
				UsefulHelpers.handleException(e, "Could not properly close socket or ScheduledExecutorService.");
				return;
			}
		}
  }

  /**
	 * Broadcast the local DV to all neighbors.
	 */
  private void broadcastLocalDV(){
    try{
      System.out.println("Updating neighbors");
      System.out.println("Sending DV[" + this.name + "]: " + this.localDV);
      // Go through all the interface addresses.
      for (InterfaceAddress broadcastAddress : this.enumInterfaceAddresses()){
        try{
          // Create a DvPacket to send to other routers.
          DvPacket bcPacket = new DvPacket(this.name, this.localDV);
          DatagramPacket sendPacket = bcPacket.toDatagram(
            broadcastAddress.getBroadcast(), this.port);
          // Send the packet.
          this.socket.send(sendPacket);

        } catch (IOException e){
          UsefulHelpers.handleException(e, "IOException occurred while trying to send packet.");
          return;
        } catch (Exception e){
          UsefulHelpers.handleException(e, "General exception occurred while trying to send packet.");
          return;
        }
      }
    } catch (SocketException e){
      UsefulHelpers.handleException(e, "SocketException occurred in broadcastDistVector when trying to enumInterfaceAddresses.");
      return;
    } catch (Exception e){
      UsefulHelpers.handleException(e, "General exception occurred in broadcastDistVector when trying to enumInterfaceAddresses.");
      return;
    }
  }

  /**
   * Update the local DV.
   *
   * Run the Bellman-Ford algorithm on each key in the local DV except for the
   * local router itself.
   */
  private void updateLocalDV(){
    for (String r : this.localDV.keySet()){
      if (!r.equals(this.name)){
        this.dvAlgorithm(r);
      }
    }
  }

  /**
	 * Implementation of the Bellman-Ford algorithm.
	 *
	 */
  public void dvAlgorithm(String y){
    int cost = BasicRouter.COST_INFINITY;
    String nextHop = "NONE";

    // Go through each neighbor.
    for (String neighbor : this.neighbors){
      // Check if the neighbor's DV contains the destination of interest.
      if (this.dvTable.get(neighbor).containsKey(y)){
        int curCost = 1 + this.dvTable.get(neighbor).get(y);
        // Check if the cost through the neighbor is the cheapest cost thus far.
        if (curCost < cost){
          updateMade = 1;
          cost = curCost;
          nextHop = neighbor;
        }
      }
    }

    // If the router is still reachable, update its new cost and the next hop
    // neighbor in the local router's forwarding table.
    if (cost < BasicRouter.COST_INFINITY){
      this.localDV.replace(y, cost);
      this.forwardingTable.replace(y, nextHop);
    } else{
      // The node is no longer reachable.
      // Remove this node from the local DV and from the forwarding table.
      this.localDV.remove(y);
      this.forwardingTable.remove(y);
    }
  }


  /**
	 * Remove inactive neighbor from the local DV table and list of neighbors.
	 *
	 */
  public void handleInactiveNeighbor(String neighbor){
    this.dvTable.remove(neighbor);
    this.neighbors.remove(neighbor);

    // Update local DV and remove all unreachable destinations from local DV
    // and forwarding tables.
    this.updateLocalDV();
  }

  /**
	 * Send a keepalive message to all neighboring routers.
	 *
	 */
  public void sendKeepaliveMessage(){
    // Broadcast local DV.
    System.out.println("Broadcasting keepalive.");
    this.broadcastLocalDV();

    // Restart keepalive timer.
    this.keepRouterAlive.cancel(true);
    this.keepRouterAlive = this.keepRouterAlivePool.schedule(
      new KeepaliveTimer(this),this.keepalive,
      TimeUnit.MILLISECONDS);
  }

	/**
	 * Signals the router to terminate
	 *
	 */
	public void shutdown(){
    this.isShutdown = 1;
  }



	/**
	 * Gets the local DV table as a Map.
	 *
	 * Each entry in the Map has a key and a value:
	 * 	key: name of a destination router
	 *	value: cost of the least-cost path to the destination specified by key
	 *
	 *@return	the local DV table at the router
	 */
	public Map<String, Integer> getDvTable(){
    return this.localDV;
  }

  /**
   * Gets the local FW table as a Map
   *
   * Each entry in the Map has a key and a value:
   * 	key: name of a destination router
   *	value: the next hop router to reach the destination specified by key
   *
   *@return	the local FW table at the router
   */
  public Map<String, String> getFwTable(){
    return this.forwardingTable;
  }
}
