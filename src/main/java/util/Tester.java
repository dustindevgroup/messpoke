package util;

import util.TimeUtils.WaitListener;

public class Tester {
	
	private static WaitListener waitListener = new WaitListener();
	
	public static void main(String[] args) {
		
		System.out.println( "start" );
		
		long timewait = 10 * 1000;
		System.out.println( "waiting for ms : " + timewait );
		TimeUtils.waitOrCancel(waitListener, timewait, waitListener);
		System.out.println( "consume = " + waitListener.consumeInterrupted() );
		
		System.out.println( "end" );
		
	}

}
