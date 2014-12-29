package io.spark.core.android.ui;

import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.storage.Prefs;
import io.spark.core.android.ui.util.Ui;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public abstract class BaseFragment extends Fragment {

	public abstract int getContentViewLayoutId();


	protected Prefs prefs;
	protected ApiFacade api;
	protected LocalBroadcastManager broadcastMgr;


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		prefs = Prefs.getInstance();
		api = ApiFacade.getInstance(activity);
		broadcastMgr = LocalBroadcastManager.getInstance(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return (ViewGroup) inflater.inflate(getContentViewLayoutId(), container, false);
	}

	/**
	 * Shows & hides the progress spinner and hides the login form.
	 */
	protected void showProgress(int viewId, final boolean show) {
		// Fade-in the progress spinner.
		int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
		final View progressView = Ui.findView(this, viewId);
		progressView.setVisibility(View.VISIBLE);
		progressView.animate()
				.setDuration(shortAnimTime)
				.alpha(show ? 1 : 0)
				.setListener(new AnimatorListenerAdapter() {

					@Override
					public void onAnimationEnd(Animator animation) {
						progressView.setVisibility(show ? View.VISIBLE : View.GONE);
					}
				});
	}


}
