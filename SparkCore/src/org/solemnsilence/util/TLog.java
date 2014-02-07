package org.solemnsilence.util;

import android.util.Log;


// "Tagged logger" -- stop having to include the log tag every time (and
// [kind of] abstract away using Android's logger directly)
public class TLog {

	private final String tag;


	public TLog(String tag) {
		this.tag = tag;
	}

	public TLog(Class<?> clazz) {
		this.tag = clazz.getSimpleName();
	}

	public void e(String msg) {
		Log.e(tag, msg);
	}

	public void e(String msg, Throwable tr) {
		Log.e(tag, msg, tr);
	}


	public void w(String msg) {
		Log.w(tag, msg);
	}

	public void w(String msg, Throwable tr) {
		Log.w(tag, msg, tr);
	}


	public void i(String msg) {
		Log.i(tag, msg);
	}

	public void i(String msg, Throwable tr) {
		Log.i(tag, msg, tr);
	}


	public void d(String msg) {
		Log.d(tag, msg);
	}

	public void d(String msg, Throwable tr) {
		Log.d(tag, msg, tr);
	}


	public void v(String msg) {
		Log.v(tag, msg);
	}

	public void v(String msg, Throwable tr) {
		Log.v(tag, msg, tr);
	}


	public void wtf(String msg) {
		Log.wtf(tag, msg);
	}

	public void wtf(String msg, Throwable tr) {
		Log.wtf(tag, msg, tr);
	}

}
