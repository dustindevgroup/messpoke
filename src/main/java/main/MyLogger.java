package main;
import org.apache.log4j.Logger;


public class MyLogger {
	
	public static Logger LOGGER = null;

	public static void init() {
		LOGGER = Logger.getLogger("MyLogger");
	}
	
}
