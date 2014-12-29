package io.spark.core.android.ui.util;

import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.util.CoreNameGenerator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.EditText;


public class NamingHelper {

	public final static String BROADCAST_NEW_NAME_FOUND = "BROADCAST_NEW_NAME_FOUND";
	public final static String EXTRA_NEW_NAME = "EXTRA_NEW_NAME";


	private final Activity activity;
	private final ApiFacade api;
	private final LocalBroadcastManager broadcastMgr;

	public NamingHelper(Activity activity, ApiFacade api) {
		this.activity = activity;
		this.api = api;
		this.broadcastMgr = LocalBroadcastManager.getInstance(activity.getApplicationContext());
	}

	public void renameCore(Device device, String newName, Runnable runOnDupeName) {
		if (DeviceState.getExistingCoreNames().contains(newName) && !newName.equals(device.name)) {
			showDupeNameDialog(runOnDupeName);
		} else {
			Intent intent = new Intent(BROADCAST_NEW_NAME_FOUND).
					putExtra(EXTRA_NEW_NAME, newName);
			broadcastMgr.sendBroadcast(intent);
			DeviceState.renameDevice(device.id, newName);
			api.nameCore(device.id, newName);
		}
	}

	public void showRenameDialog(final Device device) {
		String suggestedName = CoreNameGenerator.generateUniqueName(
				DeviceState.getExistingCoreNames());

		View dialogRoot = activity.getLayoutInflater().inflate(R.layout.dialog_rename, null);
		final EditText nameView = (EditText) dialogRoot.findViewById(R.id.new_name);
		nameView.setText(suggestedName);

		new AlertDialog.Builder(activity)
				.setView(dialogRoot)
				.setTitle(R.string.rename_your_core)
				.setPositiveButton(R.string.rename, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String newName = nameView.getText().toString();
						dialog.dismiss();
						Runnable onDupeName = new Runnable() {

							@Override
							public void run() {
								showRenameDialog(device);
							}

						};
						renameCore(device, newName, onDupeName);
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


	public void showDupeNameDialog(final Runnable runOnDupeName) {
		new AlertDialog.Builder(activity)
				.setMessage(R.string.sorry_you_ve_already_got_a_core_by_that_name_try_another_one_)
				.setPositiveButton(R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						if (runOnDupeName != null) {
							runOnDupeName.run();
						}
					}
				})
				.create()
				.show();
	}


}
