package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class KeyboardUtils {
	
	private static Map<Long, Scanner> map = new HashMap<>();
	
	public static String readString() {
		Scanner keyboardScanner = getThreadScanner();
		
		if ( keyboardScanner == null ) {
			keyboardScanner = new Scanner( System.in );
			map.put(Thread.currentThread().getId(), keyboardScanner);
		}
		String line = keyboardScanner.nextLine().toLowerCase();
		return line;
	}
	
	public static void close() {
		Scanner keyboardScanner = getThreadScanner();
		
		if ( keyboardScanner != null ) {
			keyboardScanner.close();
			keyboardScanner = null;
		}
	}

	private static Scanner getThreadScanner() {
		long threadId = Thread.currentThread().getId();
		Scanner keyboardScanner = map.get( threadId );
		return keyboardScanner;
	}
	

}
