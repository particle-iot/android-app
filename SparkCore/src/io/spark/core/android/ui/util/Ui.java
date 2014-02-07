package io.spark.core.android.ui.util;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.text.Html;
import android.view.View;
import android.widget.TextView;


public class Ui {

	@SuppressWarnings("unchecked")
	public static <T extends View> T findView(Activity activity, int id) {
		return (T) activity.findViewById(id);
	}

	@SuppressWarnings("unchecked")
	public static <T extends View> T findView(View enclosingView, int id) {
		return (T) enclosingView.findViewById(id);
	}

	@SuppressWarnings("unchecked")
	public static <T extends View> T findView(Fragment frag, int id) {
		return (T) frag.getActivity().findViewById(id);
	}

	@SuppressWarnings("unchecked")
	public static <T extends View> T findView(Dialog dialog, int id) {
		return (T) dialog.findViewById(id);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Fragment> T findFrag(Activity activity, int id) {
		return (T) activity.getFragmentManager().findFragmentById(id);
	}


	public static TextView setText(Activity activity, int textViewId, CharSequence text) {
		TextView textView = findView(activity, textViewId);
		textView.setText(text);
		return textView;
	}

	public static TextView setText(Fragment frag, int textViewId, CharSequence text) {
		TextView textView = findView(frag, textViewId);
		textView.setText(text);
		return textView;
	}

	public static String getText(Activity activity, int textViewId, boolean trim) {
		TextView textView = findView(activity, textViewId);
		String text = textView.getText().toString();
		return trim ? text.trim() : text;
	}

	public static String getText(Fragment frag, int textViewId, boolean trim) {
		TextView textView = findView(frag, textViewId);
		String text = textView.getText().toString();
		return trim ? text.trim() : text;
	}

	public static TextView setTextFromHtml(Activity activity, int textViewId, int htmlStringId) {
		TextView tv = Ui.findView(activity, textViewId);
		tv.setText(Html.fromHtml(activity.getString(htmlStringId)), TextView.BufferType.SPANNABLE);
		return tv;
	}

}
