
/**
 * A simple driver for FastFtp class
 *
 * CPSC 441
 * Assignment 3
 *
 * @author 	Majid Ghaderi
 * @version 2019
 *
 */

package cpsc441.a3.client;

import java.io.*;
import java.util.*;


public class FastFtpDriver {

	public static void main(String[] args) {

		try {
			// parse command line args
			HashMap<String, String> params = parseCommandLine(args);

			// input file name is required
			if (!params.containsKey("-f")) {
				System.out.println("incorrect usage, input file name is required");
				System.out.println("try again");
				System.exit(0);
			}

			// set the parameters
			String fileName = params.get("-f"); // name of the file to be sent to the server
			String serverName = params.getOrDefault("-s", "localhost"); // server name
			int serverPort = Integer.parseInt( params.getOrDefault("-p", "2525") ); // server port number
			int windowSize = Integer.parseInt( params.getOrDefault("-w", "10") ); // window size
			int rtoTimer = Integer.parseInt( params.getOrDefault("-t", "1000") ); // duraiton of tim-out timer

			// send the file
			FastFtp ftp = new FastFtp(windowSize, rtoTimer);

			System.out.printf("sending file \'%s\' to the server...\n", fileName);
			ftp.send(serverName, serverPort, fileName);
			System.out.println("send completed.");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}

		// get rid of any lingering threads/timers
		System.exit(0);
	}


	// parse command line arguments
	private static HashMap<String, String> parseCommandLine(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();

		int i = 0;
		while ((i + 1) < args.length) {
			params.put(args[i], args[i+1]);
			i += 2;
		}

		return params;
	}


}
