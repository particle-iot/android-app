package org.solemnsilence.util;

import android.app.Fragment;
import android.content.Context;
import android.widget.Toast;


public class Toaster {

	public static void s(Context ctx, String msg) {
		Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
	}

	public static void l(Context ctx, String msg) {
		Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
	}

	public static void s(Fragment frag, String msg) {
		s(frag.getActivity(), msg);
	}

	public static void l(Fragment frag, String msg) {
		l(frag.getActivity(), msg);
	}


}
