package io.spark.core.android.ui;

import static org.solemnsilence.util.Py.truthy;
import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.requestservice.SimpleSparkApiService;
import io.spark.core.android.storage.Prefs;
import io.spark.core.android.ui.assets.Typefaces;
import io.spark.core.android.ui.assets.Typefaces.Style;
import io.spark.core.android.ui.corelist.CoreListActivity;
import io.spark.core.android.ui.util.Ui;

import org.apache.commons.lang3.StringUtils;
import org.solemnsilence.util.TLog;
import org.solemnsilence.util.Toaster;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public abstract class BaseActivity extends Activity {

	private static final TLog log = new TLog(BaseActivity.class);


	protected Prefs prefs;
	protected ApiFacade api;
	protected LocalBroadcastManager broadcastMgr;

	private ErrorsDelegate errorsDelegate;
	private LogOutReceiver logOutReceiver;
	private DevicesUpdatedReceiver devicesUpdatedReceiver;

	private boolean isLoggingOut = false;


	/**
	 * In most of the activities in this project, the Up button should be
	 * visible if there are any Cores assigned to this account, but should NOT
	 * be visible otherwise. The exception is in {@link CoreListActivity}, which
	 * is the top of the app's hierarchy, and as such should never show the Up
	 * button.
	 * 
	 * This method exists solely to easily provide the above behavior without a
	 * lot of duplicated code.
	 * 
	 * @return
	 */
	protected boolean shouldShowUpButtonWhenDevicesListNotEmpty() {
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = Prefs.getInstance();
		api = ApiFacade.getInstance(this);
		broadcastMgr = LocalBroadcastManager.getInstance(this);
		errorsDelegate = new ErrorsDelegate(this);
		logOutReceiver = new LogOutReceiver();
		devicesUpdatedReceiver = new DevicesUpdatedReceiver();

		if (getResources().getBoolean(R.bool.lock_to_portrait)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (getActionBar() != null) {
			// This is really cheating, but the alternatives are all worse.
			int actionBarTitle = Resources.getSystem().getIdentifier("action_bar_title", "id",
					"android");
			TextView titleView = (TextView) getWindow().findViewById(actionBarTitle);
			if (titleView != null) {
				titleView.setTypeface(Typefaces.getTypeface(this, Style.LIGHT));
				titleView.setTextSize(21);
				titleView.setSingleLine(true);
			}

			// not generally a "best practice" to embed this kind of default
			// behavior in a common base class like this, but for this app's
			// needs, it works and appears to have no nasty side effects
			setCustomActionBarTitle(getString(R.string.app_name_lower));
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// action bar not guaranteed to exist.
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			if (DeviceState.getKnownDevices().isEmpty()) {
				// no known devices; disallow Up nav, but at least request the
				// devices
				getActionBar().setDisplayHomeAsUpEnabled(false);
				getActionBar().setHomeButtonEnabled(false);
				api.requestAllDevices();
			}

			CharSequence title = getActionBar().getTitle();
			if (truthy(title)) {
				setCustomActionBarTitle(title);
			}
		}

		errorsDelegate.startListeningForErrors();
		broadcastMgr.registerReceiver(logOutReceiver, logOutReceiver.getFilter());
		broadcastMgr.registerReceiver(devicesUpdatedReceiver, devicesUpdatedReceiver.getFilter());
	}

	@Override
	protected void onStop() {
		errorsDelegate.stopListeningForErrors();
		broadcastMgr.unregisterReceiver(logOutReceiver);
		broadcastMgr.unregisterReceiver(devicesUpdatedReceiver);
		super.onStop();
	}

	public void setCustomActionBarTitle(CharSequence title) {
		// TODO: remove this.
		// This doesn't do much anymore, but it's called all over the place, so
		// I'm leaving it here, at least for now.
		if (!truthy(title)) {
			log.w("Refusing to set action bar title to:'" + title + "'");
			return;
		}
		getActionBar().setTitle(title);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.all_screens, menu);
		MenuItem logOutItem = menu.findItem(R.id.action_log_out);
		logOutItem.setTitle(logOutItem.getTitle() + " " + getEllipsizedUsername());
		return true;
	}

	private String getEllipsizedUsername() {
		// this used to ellipsize using the proper unicode ellipsis character,
		// but that caused "funny character" issues, so now we do it the lame
		// but reliable way.
		return StringUtils.left(Prefs.getInstance().getUsername(), 12) + "...";
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_log_out:
				showLogOutConfirmation();
				return true;

			case R.id.action_support:
				openUri(R.string.uri_support);
				return true;

			case R.id.action_spark_homepage:
				openUri(R.string.uri_spark_homepage);
				return true;

			case R.id.action_build_your_own_core_app:
				openUri(R.string.uri_build_your_own_app);
				return true;

			case R.id.action_documentation:
				openUri(R.string.uri_docs);
				return true;

			case R.id.action_contribute:
				openUri(R.string.uri_contribute);
				return true;

			case R.id.action_report_a_bug:
				openUri(R.string.uri_report_a_bug);
				return true;
		}
		return super.onOptionsItemSelected(item);
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


	protected void openUri(int uriStringResourceId) {
		openUri(Uri.parse(
				getString(uriStringResourceId)));
	}

	protected void openUri(Uri uri) {
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}

	public ErrorsDelegate getErrorsDelegate() {
		return errorsDelegate;
	}

	private void showLogOutConfirmation() {
		new AlertDialog.Builder(this)
				.setMessage(R.string.log_out_)
				.setPositiveButton(R.string.action_log_out, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						logOut();
					}
				})
				.setNegativeButton(R.string.cancel, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.create()
				.show();
	}

	private void logOut() {
		if (!isLoggingOut) {
			isLoggingOut = true;
			startService(new Intent(this, SimpleSparkApiService.class)
					.setAction(SimpleSparkApiService.ACTION_CLEAR_INTENT_QUEUE));
			Toaster.s(this, getString(R.string.logged_out));
			prefs.clear();
			startActivity(new Intent(BaseActivity.this, LoginActivity.class));
			finish();
		}
	}

	private void onDevicesUpdated() {
		ActionBar actionBar = getActionBar();
		if (actionBar != null && shouldShowUpButtonWhenDevicesListNotEmpty()) {
			boolean noDevices = DeviceState.getKnownDevices().isEmpty();
			getActionBar().setDisplayHomeAsUpEnabled(!noDevices);
			getActionBar().setHomeButtonEnabled(!noDevices);
		}
	}


	private class LogOutReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_SHOULD_LOG_OUT);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			logOut();
		}
	}


	class DevicesUpdatedReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_DEVICES_UPDATED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			onDevicesUpdated();
		}
	}


}
