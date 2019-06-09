/**
 * UsefulHelpers Class
 *
 * @author 	Zachary Kahn
 * 
 * Some useful methods that are used by other parts of the program are
 * placed here.
 *
 */

package cpsc441.a2;

import java.util.HashMap;

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
		}
		// Terminate the worker.
		System.out.println("Terminating worker.");
	}

	/**
	 * Use to located the index of the payload (\r\n\r\n)
	 * separator within the GET request.
	 *
	 * @param pattern	The pattern being searched.
	 * @param data		The data that contains the potential pattern.
	 * @return			Index of where the pattern starts.
	 */
	public static int getIndexOfPayloadSeparator(byte[] pattern, byte[] data){

		int index = -1;
		// Search through all the data bytes.
		for (int i=0; i< data.length; i++){
			if (i+pattern.length > data.length){
				// This means that pattern was not found in the array.
				return -1;
			}
			int j;
			// Check if data[i...i+j] == pattern byte array
			for (j=0; j< pattern.length; j++){
				if (data[i+j] != pattern[j]){
					// This means the pattern was not found.
					break;
				}
			}

			if (j == pattern.length){
				// This means the pattern was found.
				index = i;
				break;
			}
		}

		return index;

	}

	/**
	 * Generates a hashmap containing the header field as the key
	 * (for example 'Host') and the information for the field as the value
	 * (for exmaple 'localhost').
	 *
	 * @param clientRequest	Request from the client.
	 * @return				Hashmap containing header field : information pairs.
	 */
	public static HashMap<String,String> requestInfo(String clientRequest){
		HashMap<String, String> info = new HashMap<String, String>();

		// Go through each line of the client's request.
		String[] headerLines = clientRequest.split("\n");
		for (int i = 0; i< headerLines.length; i++){
			if (i == 0){
				// The first line in the request is the request information.
				info.put("requestLine", headerLines[i].replace("\n", "").replace("\r", ""));
			} else if(headerLines[i].contains(":")){
				// All other lines have the form HeaderField: Info
				if (headerLines[i].contains("http")){
					// If the Host has http:// or https:// as a prefix, it is removed.
					String url = headerLines[i].split(":",2)[1].replace("\n", "").replace("\r", "").trim();
					url = removeHTTPprefix(url);
					info.put("Host", url);
				} else{
					// Otherwise, store the HeaderField as the key and the Info as the value.
					info.put(headerLines[i].split(":",2)[0].trim(), headerLines[i].split(":",2)[1].replace("\n", "").replace("\r", "").trim());
				}

			}
		}

		// If the request does not contain connection close, add it.
		if (!info.containsKey("Connection")){
			info.put("Connection", "close");
		} else if (!info.get("Connection").equals("close")){
			info.replace("Connection", "close");
		}

		return info;

	}

	/**
	 * Remove the http:// or https:// prefix from the url.
	 *
	 * @param url
	 * @return		Url with http:// or https:// prefix removed.
	 */
	private static String removeHTTPprefix(String url){
		String strippedUrl = url;
		// Remove http:// prefix
		if(url.startsWith("http://")){
			strippedUrl = url.substring("http://".length());
		} else if (url.startsWith("https://")){ // Remove https:// prefix.
			strippedUrl = url.substring("https://".length());
		}

		return strippedUrl;
	}
}
