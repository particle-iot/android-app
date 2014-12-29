package io.spark.core.android.ui;

import io.spark.core.android.R;
import io.spark.core.android.cloud.ApiFacade;

import org.solemnsilence.util.TLog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;


public class ErrorsDelegate {

	private static final TLog log = new TLog(ErrorsDelegate.class);

	private static final long MIN_DELAY_BETWEEN_DIALOGS_MILLIS = 10 * 1000;


	private final Activity activity;
	private final LocalBroadcastManager broadcastMgr;
	private final ErrorReceiver errorReceiver;

	private long lastShownUnreachableDialog = 0;
	private long lastShownHttpErrorDialog = 0;
	private long lastShownTinkerDialog = 0;


	public ErrorsDelegate(Activity activity) {
		this.activity = activity;
		this.errorReceiver = new ErrorReceiver();
		this.broadcastMgr = LocalBroadcastManager.getInstance(activity);
	}

	public void showCloudUnreachableDialog() {
		if (!canShowAnotherDialog(lastShownUnreachableDialog)) {
			log.d("Refusing to show another cloud unreachable dialog -- too soon since last one.");
			return;
		}
		lastShownUnreachableDialog = System.currentTimeMillis();
		showDialog(activity.getString(R.string.cloud_unreachable_msg));
	}

	public void showHttpErrorDialog(int statusCode) {
		if (!canShowAnotherDialog(lastShownHttpErrorDialog)) {
			log.d("Refusing to show another http error dialog -- too soon since last one.");
			return;
		}
		lastShownHttpErrorDialog = System.currentTimeMillis();
		showDialog(activity.getString(R.string.api_error_msg) + statusCode);
	}

	public void showTinkerError() {
		if (!canShowAnotherDialog(lastShownTinkerDialog)) {
			log.d("Refusing to show another tinker error dialog -- too soon since last one.");
			return;
		}
		lastShownTinkerDialog = System.currentTimeMillis();
		showDialog(activity.getString(R.string.tinker_error));
	}


	public void startListeningForErrors() {
		broadcastMgr.registerReceiver(errorReceiver, errorReceiver.getFilter());
	}

	public void stopListeningForErrors() {
		broadcastMgr.unregisterReceiver(errorReceiver);
	}


	private void showDialog(String message) {
		new AlertDialog.Builder(activity)
				.setMessage(message)
				.setPositiveButton(R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.create()
				.show();
	}

	private boolean canShowAnotherDialog(long lastShownTime) {
		return (System.currentTimeMillis() - MIN_DELAY_BETWEEN_DIALOGS_MILLIS > lastShownTime);
	}


	private class ErrorReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_SERVICE_API_ERROR);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			int errorCode = intent.getIntExtra(
					ApiFacade.EXTRA_ERROR_CODE, ApiFacade.REQUEST_FAILURE_CODE);
			if (errorCode == ApiFacade.REQUEST_FAILURE_CODE || errorCode < 300) {
				showCloudUnreachableDialog();
			} else {
				showHttpErrorDialog(errorCode);
			}
		}
	}

}
