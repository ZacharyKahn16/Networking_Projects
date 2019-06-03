
/**
 * A simple test driver.
 * 
 *
 */

import cpsc441.a1.QuickUrl;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
 
public class Driver {
	
	private static String obj1 = "http://people.ucalgary.ca/~mghaderi/cpsc441/dots.txt";
	private static String obj2 = "http://people.ucalgary.ca/~mghaderi/cpsc441/paper.pdf";
	private static String obj3 = "http://people.ucalgary.ca/~mghaderi/cpsc441/galaxy.jpg";
	
	public static void main(String[] args) {
		
		System.out.println("test started");
		System.out.println("------------\n");
		
		checkStatus(obj1, 1); // text object with single request
		checkStatus(obj2, 30); // binary object with single request
		checkStatus(obj3, 50); // big object with multiple requests
		
		// Test txt files are the same bit for bit.
		testBitForBit("dots.txt", "dotsTest.txt");
		
		// Test pdf files are the same bit for bit.
		testBitForBit("paper.pdf", "paperTest.pdf");
		
		// Test jpg files are the same bit for bit.
		testBitForBit("galaxy.jpg", "galaxyTest.jpg");
		
		System.out.println("--------------");
		System.out.println("test completed");
	}

	
	public static void checkStatus(String url, int conn) {
		QuickUrl quick = new QuickUrl();
		
		System.out.printf("url: %s\n", url);
		System.out.printf("conn: %d\n", conn);
		
		quick.setConn(conn);
		quick.getObject(url);
		
		System.out.println("done\n");
	}
	
	public static void testBitForBit(String file1, String file2){
		try{
			String localDir = new File( "." ).getCanonicalPath();

		byte[] a1result = Files.readAllBytes(Paths.get(localDir + "/"+ file1));
		byte[] storedResult = Files.readAllBytes(Paths.get(localDir + "/"+ file2));
		System.out.println("Are " + file1 +" and " + file2 + " the same bit for bit:");
		System.out.println(Arrays.equals(a1result, storedResult));
		System.out.println("");
			
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
		
	}
}
