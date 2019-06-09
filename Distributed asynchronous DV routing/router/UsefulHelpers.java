/**
 * UsefulHelpers Class
 *
 * CPSC 441
 * Assignment 4
 *
 * @author 	Zachary Kahn
 *
 * UCID: 10151534
 * Tutorial: T01
 *
 * Some useful methods that are used by other parts of the program are
 * placed here.
 *
 */

package cpsc441.a4.router;

public class UsefulHelpers {

	/**
	 * Print out the contents of an exception with a detailed message about
	 * the exception as well as other information provided by the exception itself.
	 * @param e			Exception caught.
	 * @param details	More details about the exception.
	 */
	public static void handleException(Exception e, String details){
		// Print out the details of the error.
		System.out.println(details);
		if (e != null){
			System.out.println("Error message: " + e.getMessage());
			for (StackTraceElement ele : e.getStackTrace()){
				System.out.println(ele);
			}
		}
	}
}
