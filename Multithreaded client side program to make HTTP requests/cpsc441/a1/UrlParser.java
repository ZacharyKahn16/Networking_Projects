/**
 * UrlParser
 * Parses the host name, port number, pathname, and file
 * name from a given url.
 * 
 * @author Zachary Kahn
 * 
 */

package cpsc441.a1;

public class UrlParser {

	// List of variables that need to be extracted from the inputed url.
	private String url;
	private String hostname;
	private int port;
	private String pathname; 
	private String filename;
	
	/**
	 * Constructor
	 * 
	 * @param url
	 */
	public UrlParser(String url){
		this.url = url;
	}
	
	/**
	 * Remove the http:// or https:// suffix from the url.
	 * 
	 * @param url
	 * @return		Url with http:// or https:// suffix removed.		
	 */
	public String removeHTTPsuffix(String url){
		String strippedUrl = url;
		// Remove http:// prefix
		if(url.startsWith("http://")){
			strippedUrl = url.substring("http://".length());
		} else if (url.startsWith("https://")){ // Remove https:// prefix.
			strippedUrl = url.substring("https://".length());
		}
		
		return strippedUrl;
	}
	
	/**
	 * Parses the host name, port number, pathname, and file name from the url.
	 */
	public void determineHostnameAndPathName(){
		String strippedUrl = this.removeHTTPsuffix(this.url);
		
		String[] urlParts = strippedUrl.split("/", 2);
		this.pathname = "/" + urlParts[1];
		String[] hostnameAndPort = urlParts[0].split(":");
		this.hostname = hostnameAndPort[0];
		if (hostnameAndPort.length == 1){
			this.port = 80;
		} else{
			this.port = Integer.parseInt(hostnameAndPort[1]);
		}
		// The file name is the string that follows the last forward slash.
		this.filename = urlParts[1].split("/")[urlParts[1].split("/").length-1];
	}
	
	/**
	 * @return		Host name from url.
	 */
	public String getHostname(){
		return this.hostname;
	}
	
	/**
	 * @return		Pathname from url.
	 */
	public String getPath(){
		return this.pathname;
	}
	
	/**
	 * @return 		Port number from url.
	 */
	public int getPort(){
		return this.port;
	}
	
	/**
	 * 
	 * @return		File name from url.
	 */
	public String getFileName(){
		return this.filename;
	}
	
	
}
