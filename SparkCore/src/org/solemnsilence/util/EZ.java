package org.solemnsilence.util;

import android.os.Handler;
import android.os.Looper;


// Just some painkiller/"macro" type methods that are easier to remember than
// what they implement.
public class EZ {

	public static void runOnMainThread(Runnable runnable) {
		new Handler(Looper.getMainLooper()).post(runnable);
	}


	public static void runOnMainThreadDelayed(Runnable runnable, long delayMillis) {
		new Handler(Looper.getMainLooper()).postDelayed(runnable, delayMillis);
	}

}
