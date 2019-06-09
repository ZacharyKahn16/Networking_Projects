/**
 * KeepaliveTimer Class
 *
 * CPSC 441
 * Assignment 4
 *
 * @author 	Zachary Kahn
 *
 * UCID: 10151534
 * Tutorial: T01
 *
 * Timer that is executed when the local router needs to broadcast a keepalive
 * message.
 *
 */
package cpsc441.a4.router;

public class KeepaliveTimer implements Runnable {

  // Instance of local router.
  private Router localRouter = null;

  public KeepaliveTimer(Router router){
    this.localRouter = router;
  }

  /**
   * Runs the sendKeepaliveMessage method upon a timeout.
   */
  public void run(){
    // Call the time out method in the router class to keep everything
    // synchronized.
    this.localRouter.sendKeepaliveMessage();
  }
}
