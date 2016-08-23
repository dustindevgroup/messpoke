package util;

import java.util.Random;

public class TimeUtils {

	public static class WaitListener {
		
		private boolean isInterrupted = false;
		
		public synchronized void interrupt() {
			isInterrupted = true;
		}
		
		public synchronized boolean consumeInterrupted() {
			boolean old = isInterrupted;
			isInterrupted = false;
			return old;
		}
		
	}

	private static Thread threadInput;

	public static void delay(int min, int max) {
		int next = new Random().nextInt(max) + 1;
		if (next < min) {
			next = min;
		}
		
		delay( next );
	}

	public static void delay(int seconds) {
		try {
			System.out.println("waiting " + seconds + "s");
			Thread.sleep( seconds * 1000 );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void waitForInput(final WaitListener listener) {
		if ( threadInput != null ) {
			return;
		}
		
		threadInput = new Thread(new Runnable() {
			
			@Override
			public void run() {
				String string = KeyboardUtils.readString();
				if ( "x".equals(string) ) {
					listener.interrupt();
					synchronized (listener) {
						listener.notifyAll();
					}
				}
			}
		});
		
		threadInput.start();
	}
	
	public static void waitOrCancel(Object o, long millis, final WaitListener listener) {
		synchronized (o) {
			try {
				waitForInput( listener );
				
				System.out.println("waitOrCancel " + millis + "ms");
				o.wait( millis );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
