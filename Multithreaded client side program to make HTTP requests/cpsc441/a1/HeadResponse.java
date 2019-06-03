/**
 * HeadResponse
 * Checks the HTTP HEAD response contains the Accept-Ranges field.
 * If it does, it finds the Content-Length of the requested object.
 * If it does not, -1 is returned.
 * 
 * @author Zachary Kahn
 * 
 */

package cpsc441.a1;

public class HeadResponse {

	// HTTP HEAD response
	private String response;
	
	/**
	 * Constructor
	 * @param response HTTP HEAD response
	 */
	public HeadResponse(String response){
		this.response = response;
	}
	
	/**
	 * Checks if HEAD response has the Accept-Ranges field.
	 * @return 		True if response contains Accept-Ranges field.
	 */
	private boolean checkAcceptRanges(){

		return this.response.contains("Accept-Ranges: bytes");
		
	}
	
	/**
	 * Get the content length of the requested object.
	 * @return		Content length of object, if none given return -1.
	 */
	public int getRange(){
		
		// Check if 404 or other request error occurs.
		if (this.response.contains("404 NOT FOUND") || this.response.contains("404 Not Found") ){
			System.out.println("Request produced a 404 NOT FOUND.");
			System.out.println("Terminating program.");
			System.exit(0);
		} else if(!this.response.contains("200 OK")){
			System.out.println("Request did not contain 200 OK.");
			System.out.println("Terminating program.");
			System.exit(0);
		}
		
		// Check if range requests are accepted.
		if (!this.checkAcceptRanges()){ 
			return -1;
		}
		
		String possibleRange = null;
		// Check if HEADER response contains the "Content-Length:" field. 
		for (String res : this.response.split("\n")){
			if (res.startsWith("Content-Length:")){
				possibleRange = res;
			}
		}
		
		String[] pieces = possibleRange.split(":");
		
		String range = pieces[1].trim();
			
		if (range.equalsIgnoreCase("none")){ // No valid range is available.
			return -1;
		}
		
		// Otherwise, range must be an integer.
		return Integer.parseInt(range);
		
	}
}
