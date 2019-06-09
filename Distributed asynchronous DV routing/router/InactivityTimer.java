/**
 * InactivityTimer Class
 *
 * @author 	Zachary Kahn
 *
 * Timer that is executed when a neighboring router has not responded within
 * the inactivityInterval time window.
 *
 */
package cpsc441.a4.router;

public class InactivityTimer implements Runnable {

  // Instance of the local router and timed out neighbor.
  private Router localRouter = null;
  private String timedOutNeighbor = null;

  /**
	 * Constructor
	 *
	 * @param router			Instance of local router.
	 * @param routerPort	Name of neighboring router.
	 */
  public InactivityTimer(Router router, String n){
    this.localRouter = router;
    this.timedOutNeighbor = n;
  }

  /**
   * Runs the handleInactiveNeighbor method upon a timeout.
   */
  public void run(){
    // Call the time out method in the router class to keep everything
    // synchronized.
    this.localRouter.handleInactiveNeighbor(this.timedOutNeighbor);
  }
}
