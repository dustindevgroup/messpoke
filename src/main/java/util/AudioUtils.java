package util;

import java.awt.Toolkit;

public class AudioUtils {

	public static void beepSound() {
		Toolkit.getDefaultToolkit().beep();
	}

	public static void beepSound(int times) {
		for (int i = 0; i < times; i++) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			beepSound();
		}
	}

}
